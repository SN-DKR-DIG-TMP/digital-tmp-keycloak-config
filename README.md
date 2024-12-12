# Digital-tmp-keycloak-config /!\ WORK IN PROGRESS /!\
Ce projet contient la librairie keycloak avec la configuration nécessaire pour faciliter l'intégration de keyclaok.
Il met aussi à disposition la quasi totalité des services proposés par keaycloak, la gestion des realms, la gestions des utulisateurs, roles...
Ce module permet de rendre abstraite la creation d'utilisateur, role, etc... depuis keycloak

## Prerequisites
- Keycloak version 23 et plus recommendé
- java 21 ou plus 
- Maven

## Configuration

### Intégrer la dépendence avec la bonne version
```xml		
<dependency>
   <groupId>net.atos.digital</groupId>
   <artifactId>digital-tmp-keycloak-config</artifactId>
   <version>${keycloak-config.version}</version>
</dependency>
```
### Création de la classe de configuration
- Pour intégrer cette librairie dans un projet, vous avez besoin de créer la classe de configuration `KeycloakUserConfiguration`

```java
    @Configuration
    @EnableKeycloakConfig
    public class KeycloakUserConfiguration {
        ...
    }
```

### Définir les propriéttés avec les bonnes valeurs
```yaml
 keycloakadminclient:
  realm: coffreFortAdmin
  server-url: keycloak server url exemple http://localhost:8080/
  http:
    poolsize: 5
    sockettimeout: 5000
    establishConnectionTimeout: 5000
    connectionCheckoutTimeout: 5000
  proxy:
    enable: false
    host: proxy host
    port: proxy port
  master:
    realm:  realm mmaster
    clientId: admin-cli
    user: admin
    password: admin
  admin:
    realm: realm admin
    clientId: admin-cli
    user: admin
    password: admin
  users:
    maxResult: 999999 
    
  keycloak:
    users:
      active: true  
  account:
    creation:
      sendEmail: false
      emailActionsList: OTP_CONFIGURE 
```

### Configuraton du model de données
- Votre entité `UserEntity` doit étendre la classe `UserEntityModel`

```java
    @Data
    @Entity
    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    @Table(name = "Users")
    @EntityListeners(AuditingEntityListener.class)
    @TableGenerator(name = "UserGen", table = "JPA_SEQUENCES", pkColumnName = "SEQ_KEY", valueColumnName = "SEQ_VALUE", pkColumnValue = "UserId", initialValue = 0, allocationSize = 1)
    public class UserEntity extends UserEntityModel implements Serializable {

        @Id
        @Column(name = "UserId", unique = true, nullable = false)
        @GeneratedValue(strategy = GenerationType.TABLE, generator = "UserGen")
        private Long userId;
        
        ...
    }
```
- Votre DTO `User` doit étendre la classe `UserDtoModel`

```java
    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class User extends UserDtoModel {
        ...
    }
```

La classe `UserDtoModel` contient déjà les attributs:
- `userKeycloakId` Identifiant de l'utilisateur sur keycloak
- `userId` L'identifiant de l'utilisateur dans la base de données
- `enable` Pour dire si l'utilisateur est activé ou désactivé sur keycloak

Voici la structure de la classe

```java
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    public class UserDtoModel {
    
        String userKeycloakId;
    
        Long userId;
    
        boolean enable;
    }
```

### Création de `UserDTO`

Vous devez créer une classe de mapper qui vous permet de mapper de Entity en DTO et inversement
Et aussi de créer l'objet UserKeycloak dont la structure est: 

```java
    public record UserKeycloak(

        String userFirstName,

        String userLastName,

        String userEmailAddress,

        @NotEmpty
        String username,

        String userKeycloakId,

        String userLocale,

        String roleName,

        String userPassword,

        String userRealm

    ) {
    }
```

- Exemple de classe UserMapper en utilisant mapstruct

```java
    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
    public abstract class UserMapper implements EntityMapper<User, UserDto> {
    
        @Autowired
        protected RealmProperties realmProperties;
    
        @Mapping(target = "roleName", source = "role.name")
        @Mapping(target = "userRealm", expression = "java(realmProperties.realm())")
        public abstract UserKeycloak asUserKeycloak(UserDto user);
    
    }
```

### Activation du pack

#### exemple d'implémentation de `AbstractUserService`
```java


import net.atos.digital.keycloak.exceptions.ResourceNotFoundException;
import net.atos.digital.keycloak.models.UserKeycloak;
import net.atos.digital.keycloak.services.AbstractUserService;

import org.springframework.stereotype.Service;

import java.text.MessageFormat;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakUserServiceImpl implements AbstractUserService<UserEntity, User> {

    final UserRepository userRepository;

    final UserMapper userMapper;

    static final String USER = "user";

    @Override
    
    // Turn your UserDTO into a UserKeycloak
    public UserKeycloak asUserKeycloak(User userDto) {
        return userMapper.asUserKeycloak(userDto);
    }

    @Override
    // Check for the existence of a user with this username
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    // Check for the existence of a user with this e-mail address
    public boolean existsByUserEmailAddress(String userEmailAddress) {
        return userRepository.existsByUserEmailAddress(userEmailAddress);
    }

    @Override
    // Saving the user in our database
    public User saveUser(User userDto) {
        UserEntity userEntity = userMapper.asEntity(userDto);
        return userMapper.asDto(userRepository.save(userEntity));
    }

    @Override
    // Retrieve a user by user ID
    public User readUserByUserId(Long userId) {
        return userMapper.asDto(userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(USER, userId)));

    }

    @Override
    // Deleting the user from the database and also from keycloak
    public User deleteUser(Long userId) {
        /* Getting user */
        var user = userMapper.asDto(userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(USER, userId)));

        /* Deleting entity */
        userRepository.deleteById(userId);

        return user;
    }

    @Override
    // Check if there is another user with this e-mail address other than him
    public boolean existsByUserEmailAddressAndUserIdNot(String userEmailAddress, Long userId) {
        return userRepository.existsByUserEmailAddressAndUserIdNot(userEmailAddress, userId);
    }
}

```

### Mettre à jour la classe de configuration de keycloak 

```java
    @Configuration
    @EnableKeycloakConfig
    public class KeycloakUserConfiguration {
        @Bean
        public KeycloakUserService<User, UserDto> getKeycloakUserService(AbstractUserServiceImpl userService, UserMapper userMapper, KeycloakService keycloakService, UserManagerConfiguration userManagerConfiguration) {
            return new KeycloakUserService<>(userService, userMapper, keycloakService, userManagerConfiguration);
        }
    }
```

### Exemple d'intégration de `UserService` et `RoleService`



```java
    @Service
    @RequiredArgsConstructor
    @Slf4j
    @Transactional
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class UserServiceImpl implements UserService {
    
        final PasswordUtils passwordUtils;
    
        final UserRepository userRepository;
    
        final RoleRepository roleRepository;
    
        final UserMapper userMapper;
    
        final RoleMapper roleMapper;
    
        final RealmProperties realmProperties;
    
        static final String USER = "user";
    
        static final String ROLE_NOT_FOUND_MESSAGE = "Role not found with the id: {0}";
        static final String ROLE_NOT_FOUND_BY_NAME_MESSAGE = "Role not found with the name: {0}";
    
        final KeycloakUserService<User, UserDto> keycloakUserService;
    
        @Override
        public UserDto createUser(UserDto userDto) {
            log.info("Creating user with details: {}", userDto);
            /* Getting role by id or name*/
            var role = getRole(userDto.getRole());
    
            if (StringUtils.isEmpty(userDto.getUserPassword())) {
                userDto.setUserPassword("Password"); // To be added if you want to give the user a default password
            }
    
            /* Enable by default */
            userDto.setEnable(true);
    
            /* Setting role */
            userDto.setRole(role);
    
            return keycloakUserService.createUser(realmProperties.realm(), realmProperties.clientId(), userDto);
    
        }
    
        @Override
        public void updatePassword(Long userId, String password) {
            log.info("Updating password for user with userId: {}", userId);
            keycloakUserService.updatePassword(userId, password);
        }
    
    
        @Override
        @Transactional(readOnly = true)
        public UserDto readUserByUserId(Long userId) {
            log.info("Reading user by userId: {}", userId);
    
            /* Getting user */
            return userRepository.findByUserId(userId).map(userMapper::asDto).orElseThrow(() -> new ResourceNotFoundException(USER, userId));
    
        }
    
    
        @Override
        public UserDto readUserByUserKeycloakId(String userKeycloakId) {
            log.info("Reading user by userKeycloakId: {}", userKeycloakId);
    
            /* Getting user */
            return userMapper.asDto(userRepository.findByUserKeycloakId(userKeycloakId)
                    .orElseThrow(() -> new ResourceNotFoundException(USER, userKeycloakId)));
        }
    
        @Override
        public UserDto readUserByUsername(String username) {
            log.info("Reading user by username: {}", username);
    
            /* Getting user */
            return userMapper.asDto(userRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new ResourceNotFoundException(USER, username)));
        }
    
        @Override
        public Page<UserDto> readUsers(String email, String firstName, String lastName, List<String> roles, List<String> exceptedRoles, Pageable pageable) {
    
            log.info("Fetching users with filters - email: {} - firstName: {} - lastName: {} - role: {}", email, firstName, lastName, roles);
    
            /* Getting roles */
            var users = userRepository.findByFilters(email, firstName, lastName, roles, exceptedRoles, pageable).map(userMapper::asDto);
    
            log.trace("Users fetched: {}", users);
    
            return users;
        }
    
        @Override
        public UserDto updateUser(UserDto userDto) {
            log.info("Updating user details: {}", userDto);
    
            /* Setting role */
            userDto.setRole(getRole(userDto.getRole()));
    
            /* Checking if ID exists */
            var userDb = readUserByUserId(userDto.getUserId());
            userDto.setUserKeycloakId(userDb.getUserKeycloakId());
            userDto.setUsername(userDb.getUsername());
    
            return keycloakUserService.updateUser(userDto);
        }
    
        @Override
        public void deleteUser(Long userId) {
            log.info("Deleting user with userId: {}", userId);
            keycloakUserService.deleteUser(userId);
        }
    
        @Override
        // Send an e-mail to a user about an action:
        //- VERIFY_MAIL: verification mail when user is created to activate account
        //- UPDATE_PASSWORD: Change password on first login
        //- OTP_CONFIGURE: Mail to configure 2FA authentication with authenticator
        public void sendEmailActions(Long userId, List<String> emailActionsList) {
            log.info("Sending email actions to user with userId: {}", userId);
            keycloakUserService.sendEmailActions(userId, realmProperties.clientId(), emailActionsList);
        }
    
        @Override
        // Account activation or deactivation on keycloak
        public UserDto disableOrEnableKeycloakUser(Long userId, boolean isEnable) {
    
            log.info("disable Or enable user: {}", userId);
    
            /* Checking if ID exists */
            var userDto = readUserByUserId(userId);
    
            userDto.setEnable(isEnable);
            return keycloakUserService.disableOrEnableKeycloakUser(userDto);
    
        }
    
        private RoleDto getRole(RoleDto userRole) {
            if (userRole.getRoleId() != null) {
                return roleMapper.asRole(roleRepository.findById(userRole.getRoleId()).orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND_MESSAGE, userRole.getRoleId())));
            } else {
                return roleMapper.asRole(roleRepository.findByName(userRole.getName()).orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND_BY_NAME_MESSAGE, userRole.getName())));
            }
        }
    }
```

```java
    @Slf4j
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Service
    @RequiredArgsConstructor
    public class RoleServiceImpl implements RoleService {
        static final String ROLE = "Role";
        static final String ROLE_NAME = "roleName";
        static final String ROLE_IN_USE = "Role with name: {0} is already affected to one or many users.";
        static final String ROLE_NAME_UPDATE_FORBIDDEN = "Role name cannot be updated.";
    
        final RoleRepository roleRepository;
    
        final RoleMapper roleMapper;
    
        final KeycloakService keycloakService;
    
        final UserManagerConfiguration userManagerConfiguration;
    
        final UserRepository userRepository;
    
        final RealmProperties realmProperties;
    
    
        @Override
        @Transactional
        public RoleDto createRole(RoleDto roleDto) {
    
            /* Checking if resource already exists */
            if (roleRepository.existsByName(roleDto.getName())) {
                throw new ResourceAlreadyExistException(ROLE, ROLE_NAME, roleDto.getName());
            }
    
            /* Saving KEYCLOAK ROLE */
            if (userManagerConfiguration.isKeycloakUsersActive) {
                var roleRepresentation = keycloakService.createKeycloakRole(realmProperties.realm(), roleDto.getName(), roleDto.getDescription());
                roleDto.setExternalReference(roleRepresentation.getId());
            }
    
            try {
    
                /* Saving role in module */
                return roleMapper.asRole(roleRepository.save(roleMapper.asRole(roleDto)));
    
            } catch (Exception ex) {
    
                /* Deleting role in IAM */
                keycloakService.deleteKeycloakRole(realmProperties.realm(), roleDto.getName());
                throw new ResourceNotFoundException("Error creating role", ex);
            }
    
        }
    
        @Override
        public RoleDto readRoleByRoleId(Long roleId) {
    
            /* Getting Role */
            var role = roleMapper.asRole(roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException(ROLE, roleId)));
    
            log.info("readRoleByRoleId end ok - roleId: {}", roleId);
    
            return role;
        }
    
        @Override
        @Transactional(readOnly = true)
        public RoleDto readRoleByRoleName(String roleName) {
    
            /* Getting role */
            var role = roleMapper.asRole(roleRepository.findByName(roleName).orElseThrow(() -> new ResourceNotFoundException(ROLE, roleName)));
    
            log.info("readRoleByRoleName end ok - roleName: {}", roleName);
            log.trace("get role by name was ok - role: {}", role);
    
            return role;
        }
    
        @Override
        @Transactional(readOnly = true)
        public Page<RoleDto> readRolesByFilters(String name, String description, Pageable pageable) {
    
            /* Getting roles */
            var roles = roleRepository.findByFilters(name, description, pageable).map(roleMapper::asRole);
    
            log.debug("readRolesByFilters end ok -  name: {} - description: {}", name, description);
            log.trace("readRolesByFilters end ok - roles: {}", roles);
    
            return roles;
        }
    
        @Override
        public RoleDto updateRole(RoleDto roleDto) {
    
            /* Getting role*/
            var existingRole = readRoleByRoleId(roleDto.getRoleId());
    
            /* Checking if name has changed */
            if (!existingRole.getName().equals(roleDto.getName())) {
                throw new ForbiddenActionException(HttpStatus.FORBIDDEN, ROLE_NAME_UPDATE_FORBIDDEN);
            }
    
    
            /* Setting external reference */
            roleDto.setExternalReference(existingRole.getExternalReference());
    
            /* Saving entity */
            var updatedRole = roleMapper.asRole(roleRepository.save(roleMapper.asRole(roleDto)));
    
            log.info("updateRole end ok -  name: {}", roleDto.getName());
            log.trace("updateRole end ok - roles: {}", updatedRole);
    
    
            return updatedRole;
        }
    
    
        @Override
        public void deleteRole(Long roleId) {
    
            /* Getting role */
            var role = readRoleByRoleId(roleId);
    
            if (userRepository.existsAllByRoleRoleId(roleId)) {
                throw new ForbiddenActionException(HttpStatus.FORBIDDEN, MessageFormat.format(ROLE_IN_USE, role.getName()));
            }
    
            if (Boolean.TRUE.equals(userManagerConfiguration.isKeycloakUsersActive)) {
                keycloakService.deleteKeycloakRole(realmProperties.realm(), role.getName());
            }
    
            roleRepository.deleteById(roleId);
    
            log.info("deleteRole end ok -  role: {}", role);
            log.trace("deleteRole end ok - role: {}", role);
        }
    
        @Override
        @Transactional(readOnly = true)
        public Collection<String> getPermissions(String roleName) {
    
            if (roleRepository.findByName(roleName).isEmpty()) {
                return Collections.emptyList();
            }
    
            /* Getting role */
            var role = readRoleByRoleName(roleName);
    
            return role.getPermissions();
        }
    
        @Override
        public RoleDto addPermission(String roleName, String permission) {
            /* Getting role */
            var role = roleMapper.asRole(roleRepository.findByName(roleName).orElseThrow(() -> new ResourceNotFoundException(ROLE, roleName)));
    
            if (role.getPermissions().contains(permission)) {
                log.info("Role {} already contains permission {}", roleName, permission);
                return role;
            }
    
            role.getPermissions().add(permission);
    
            return updateRole(role);
        }
    
        @Override
        public RoleDto deletePermission(String roleName, String permission) {
            /* Getting role */
            var role = roleMapper.asRole(roleRepository.findByName(roleName).orElseThrow(() -> new ResourceNotFoundException(ROLE, roleName)));
    
            if (!role.getPermissions().contains(permission)) {
                log.info("Role {} does not contains permission {} to remove", roleName, permission);
                return role;
            }
    
            role.getPermissions().remove(permission);
    
            return updateRole(role);
        }
    
        @Override
        @Transactional(readOnly = true)
        public List<String> getPermissionsByRoleName(String roleName) {
            return (List<String>) roleRepository.findByName(roleName).map(Role::getPermissions).orElseThrow();
        }
    }
```