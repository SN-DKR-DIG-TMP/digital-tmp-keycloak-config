# Digital-tmp-keycloak-config /!\ WORK IN PROGRESS /!\
Ce projet contient la librairie keycloak avec la configuration nécessaire pour faciliter l'intégration de keyclaok.
Il met aussi à disposition la quasi totalité des services proposés par keaycloak, la gestion des realms, la gestions des utulisateurs, roles...

## Prerequisites
Keycloak version 23 et plus recommendé
java 17 ou plus 
Maven

## Configuration
### Intégrer la dépendence avec la bonne version 
```xml		
<dependency>
   <groupId>net.atos.digital</groupId>
   <artifactId>digital-tmp-keycloak-config</artifactId>
   <version>0.0.1-SNAPSHOT</version>
</dependency>
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

### Implémenter les classes `AbstractUserService` et `AbstractRoleService`
#### exemple d'implémentation de `AbstractUserService`
```java
package net.atos.digital.coffrefort.service;

import net.atos.digital.coffrefort.mapper.UserMapper;
import net.atos.digital.coffrefort.model.dto.User;
import net.atos.digital.coffrefort.model.entity.UserEntity;
import net.atos.digital.coffrefort.repository.UserRepository;
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
public class AbstractUserServiceImpl implements AbstractUserService<UserEntity, User> {

    final UserRepository userRepository;

    final UserMapper userMapper;

    final CustomerContext customerContext;

    static final String USER = "user";

    @Override
    public UserKeycloak asUserKeycloak(User userDto) {
        return userMapper.asUserKeycloak(userDto);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByUserEmailAddress(String userEmailAddress) {
        return userRepository.existsByUserEmailAddress(userEmailAddress);
    }

    @Override
    public User saveUser(User userDto) {
        UserEntity userEntity = userMapper.asEntity(userDto);
        return userMapper.asDto(userRepository.save(userEntity));
    }

    @Override
    public User readUserByUserId(Long userId) {
        return userMapper.asDto(userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(USER, userId)));

    }

    @Override
    public User deleteUser(Long userId) {
        /* Getting user */
        var user = userMapper.asDto(userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(USER, userId)));

        /* Deleting entity */
        userRepository.deleteById(userId);

        return user;
    }

    @Override
    public boolean existsByUserEmailAddressAndUserIdNot(String userEmailAddress, Long userId) {
        return userRepository.existsByUserEmailAddressAndUserIdNot(userEmailAddress, userId);
    }
}

```




