CREATE database personify_scim;
GRANT usage on *.* to 'personify'@'%' identified by 'azerty1234';
GRANT ALL PRIVILEGES ON *.* TO 'personify'@'localhost' IDENTIFIED BY 'azerty1234' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'personify'@'%' IDENTIFIED BY 'azerty1234' WITH GRANT OPTION;

FLUSH PRIVILEGES;
