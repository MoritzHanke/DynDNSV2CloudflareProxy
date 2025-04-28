# DynDNSV2CloudflareProxy (Work in progress)
The Project is still Work in Progress. Once finished this will be removed...<br><br>
Accepts DynDnsV2 Updates and redirects them to Cloudflare. 
Uses [NanoHTTP](https://github.com/NanoHttpd/nanohttpd) with maven for hosting the network and [org.json](https://github.com/douglascrockford/JSON-java) for JSON de/-serialization.

## Usage
Compile the program or download the [FAT-JAR](#fat-jar)/ [JAR with dependencies](#jar-with-dependencies) from [Releases](#releases).

Define a config and log file with the `` CONFIG_PATH`` und ``LOG_PATH`` environment variables 
or let them generate at ``./proxy.conf`` and ``./dyndnsproxy.log`` by running the program once.
The ``config-file`` should look something like this:
````lombok.config
BasicAuth_active=true
BasicAuth_password=
BasicAuth_username=
Cloudflare_API_KEY=
Cloudflare_EMAIL=
Cloudflare_ZONE_ID=
HTTPS_active=false
HTTPS_keyStoreFilePath=
HTTPS_keyStorePassphrase=
HTTPS_port=443
HTTP_active=false
HTTP_port=80
IPAddress=127.0.0.1
LogTime2Comments=true
````


Where

| Option                       | Description                                                                                                                                                                                                                                                                        |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ``BasicAuth_active``         | ``true``: use the 'Basic' authentication method.<br/>``false``: dont authenticate (only use in trusted networks)                                                                                                                                                                   |
| ``BasicAuth_password``       | password for authentication                                                                                                                                                                                                                                                        |
| ``BasicAuth_username``       | username for authentication                                                                                                                                                                                                                                                        |
| ``Cloudflare_API_KEY``       | Only the [GLOBAL API TOKEN](https://dash.cloudflare.com/profile/api-tokens) works, although maybe you can get another token running with a bit trial and error                                                                                                                     |
| ``Cloudflare_EMAIL``         | Your Cloudflare Email                                                                                                                                                                                                                                                              |
| ``Cloudflare_ZONE_ID``       | You can find your ZONE_ID under ``Account Home > 'SELECT DOMAIN' > 'SCROLL (in Overview) DOWN. SHOULD BE SOMEWHERE ON THE RIGHT' ``                                                                                                                                                |
| ``HTTPS_active``             | ``true``: run HTTPS network on port ``HTTPS_port``                                                                                                                                                                                                                                 |
| ``HTTPS_keyStoreFilePath``   | Filepath to keystore-file, containing a certificate and private key. <br/>Depending on your setup a self-signed Certificate may be enough. <br/>(ex. ``D:\\test\\cert.jks`` or ``/etc/certs/cert.jks``). Has many shenanigans (needs same java version, maye other things too)     |
| ``HTTPS_keyStorePassphrase`` | Passphrase for keystore file                                                                                                                                                                                                                                                       |
| ``HTTPS_port``               | Port on which the HTTPS network will listen                                                                                                                                                                                                                                        |
| ``HTTP_active``              | ``true``: run HTTP network on port ``HTTP_port`` (be careful with authentication credentials)                                                                                                                                                                                      |
| ``HTTP_port``                | Port on which the HTTPS network will listen                                                                                                                                                                                                                                        |
| ``IPAddress``                | IP-Address the HTTP or HTTPS network is listening on.<br/>Use ``0.0.0.0`` or so if it should be accessible from outside.                                                                                                                                                           |
| ``LogTime2Comments``         | ``true``: sets the current time as a comment when updating records. Although a ``last-changed`` attribute can be obtained anyway somehow                                                                                                                                           |

Both ``HTTP`` and ``HTTPS`` may be used at the same time if needed.

TODO: configure Router, Wildcard + weird escape explanation. everything is case insensitive

### Docker
Docker-Integration should be relative straight forward. <br>
The ``Dockerfile`` and ``docker-compose.yml`` contain an example using the ``openjdk:8-alpine`` base image and ports ``5000`` and ``5001`` in the config.

TODO: update before release to a working example


## Supported Features
Supports Raw HTTP Requests as specified in https://help.dyn.com/remote-access-api/perform-update/. <br>
Returns Return-Codes as specified in https://help.dyn.com/remote-access-api/return-codes/.
### Added/Missing/Changed Features
(will probably not be implemented, unless I need them at some point)
- ``offline`` Update Parameter is ignored
- URL-Authentication is not supported
- Accepts every URI (Header, ex. /test/bla)
- Accepts all Request-Types (GET, POST, ...)
- Does not validate Host (Header)
- no limit on hostnames (although cloudflare can only load 5000000 in one page, so 5000000 it is)
- wildcard for hostnames (see [Usage](#usage))
- if the hostname specified is not a fully-qualified domain name, ``nohost`` will be returned instead of ``notfqdn``

## Releases
None so far lol
### Fat JAR

### JAR with dependencies
