#!/bin/bash

sed -i~ "/<servers>/ a\
<server>\
  <id>central</id>\
  <username>${mvn_user}</username>\
  <password>${mvn_password}</password>\
</server>" /usr/share/maven/conf/settings.xml

sed -i "/<profiles>/ a\
<profile>\
  <id>central</id>\
  <activation>\
    <activeByDefault>true</activeByDefault>\
  </activation>\
  <repositories>\
    <repository>\
      <id>central</id>\
      <url>https://registry.kloudspot.com/repository/maven-public/</url>\
    </repository>\
  </repositories>\
  <pluginRepositories>\
    <pluginRepository>\
      <id>central</id>\
      <url>https://registry.kloudspot.com/repository/maven-public/</url>\
    </pluginRepository>\
  </pluginRepositories>\
</profile>" /usr/share/maven/conf/settings.xml
