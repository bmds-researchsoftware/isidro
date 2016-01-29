## ISIDRO-WEB

This is the web interface for the ISIDRO project.

To run:

* Run the command "./activator ui" in the project folder.
* Run the application from activator's browser window

By default, a postgres database will be used:

* Install postgres
* Create a database for isidro (default: isidro)
* Create a user for isidro (default: playdbuser)
    * Make sure to use an encrypted password: 
```
#!sql

       ALTER USER playdbuser with encrypted password 'xyzzy123';
```
* In the file postgresql.conf, uncomment the line: 
```
#!conf

       listen_addresses = 'localhost'
```

* Restart the database server