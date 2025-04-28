# DynDNSV2CloudflareProxy
Accepts DynDnsV2 Updates and redirects them to Cloudflare. 
Uses [NanoHTTP](https://github.com/NanoHttpd/nanohttpd) for hosting the server and 
[org.json](https://github.com/douglascrockford/JSON-java) for JSON de/-serialization.
Build with maven, FAT-Jar also available.

## Quick Start
1. Download the [FAT-Jar](#fat-jar) 
2. execute it once to generate the ``proxy.conf`` file in your **cwd**
(or use the ENV ``CONFIG_PATH=/test/x.cfg``(or ``D:\\test\\x.cfg``) for a custom path; similar with ``LOG_PATH``).
3. Set config values :

(obtain the [API-Token](https://dash.cloudflare.com/profile/api-tokens) and ZONE_ID (on the right in "Overview" when managing your zone) from cloudflare)<br>
(maybe change ``HTTPS_port`` to some custom port, eg. ``5001``)
```properties
BasicAuth_active=true
BasicAuth_password=your_password
BasicAuth_username=your_username
Cloudflare_TOKEN=your_api_token_with_EditZoneDNS_rights
Cloudflare_ZONE_ID=your_zone_id
HTTPS_active=true
HTTPS_keyStoreFilePath=
HTTPS_keyStorePassphrase=
HTTPS_keyStore_genSelfSigned=true
HTTPS_port=443
HTTP_active=false
HTTP_port=80
IPAddress=0.0.0.0
LogTime2Comments=true
````
4. Run the Jar-File again, should print something like ``"HTTPs-Proxy started. Listening on 0.0.0.0:5001""``
5. Configure your router (or other device that sends DynDnsV2 requests).
    - use Basic-Auth ``your_username`` and ``your_password``
    - ``Hostname`` may contain comma-seperated hostnames (if supported by device). <br>
   ``@`` at the beginning of a hostname acts like a wildcard. 
   <br>Escaping with ``'X'``: ``X@...`` -> ``"@..."``, ``XX@...`` -> ``"X@..."``, ...
   <br> Ex. ``bla.test.com,test.com,test.test.com`` or just ``@test.com``
    - Use protocol ``HTTPS`` and port ``HTTPS_port`` (from Config)
    - ``update-server-address`` should be the address under which your pc can be contacted <br>
      (if your pc is in the same network as your router, you can often use the device-name instead of the local ipv4). <br>
   You can find your local IPv4 by running ``ipconfig``/``ifconfig``.

Hopefully everything should work now lol
### Docker
1. Download the [FAT-Jar](#fat-jar) and the [Dockerfile + docker-compose](#docker-example-files) and save them into the same directory.
2. run ``docker compose up`` inside this directory.
3. Edit the config under ``./proxy_files/proxy.conf`` in the same way as above in Step 3. To change the ``HTTPS-port``, modify the `5001` inside the `docker-compose.yml`.
4. run ``docker compose up`` and close the terminal if no error occurred (or run with the ``-d`` flag)
5. Configure your router in the same way as above in Step 5.

## Usage
Compile the program or download the [FAT-JAR](#fat-jar)/ [JAR with dependencies and POM](#jar-with-dependencies)from [Releases](#releases).

Define a config and log file with the `` CONFIG_PATH`` und ``LOG_PATH`` environment variables 
or let them generate at ``./proxy.conf`` and ``./dyndnsproxy.log`` by running the program once.
The ``config-file`` should look something like this:
````properties
BasicAuth_active=true
BasicAuth_password=
BasicAuth_username=
Cloudflare_TOKEN=
Cloudflare_ZONE_ID=
HTTPS_active=false
HTTPS_keyStoreFilePath=
HTTPS_keyStorePassphrase=
HTTPS_keyStore_genSelfSigned=true
HTTPS_port=443
HTTP_active=false
HTTP_port=80
IPAddress=127.0.0.1
LogTime2Comments=true
````


Where

| Option                           | Description                                                                                                                                                                                                                 |
|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ``BasicAuth_active``             | ``true``: use the 'Basic' authentication method.<br/>``false``: dont authenticate (authentication-headers are ignored) (only use in trusted networks)                                                                       |
| ``BasicAuth_password``           | (optional, if ``BasicAuth_active=false``) password for authentication                                                                                                                                                       |
| ``BasicAuth_username``           | (optional, if ``BasicAuth_active=false``) username for authentication                                                                                                                                                       |
| ``Cloudflare_TOKEN``             | an [API-Token](https://dash.cloudflare.com/profile/api-tokens) with `Edit_DNS_Zones` rights.                                                                                                                                |
| ``Cloudflare_ZONE_ID``           | You can find your ZONE_ID under ``Account Home > 'SELECT DOMAIN' > 'SCROLL (in the `Overview` tab) DOWN (should be somewhere on the right))' ``                                                                             |
| ``HTTPS_active``                 | ``true``: run HTTPS server on port ``HTTPS_port``                                                                                                                                                                           |
| ``HTTPS_keyStoreFilePath``       | (optional, if ``HTTPS_active=false`` or ``HTTPS_keyStore_genSelfSigned=true``) Filepath to keystore-file, containing a certificate + private key. Must be created in the same JAVA-Version that runs the JAR                |
| ``HTTPS_keyStorePassphrase``     | (optional, if ``HTTPS_active=false`` or ``HTTPS_keyStore_genSelfSigned=true``) Passphrase for keystore file                                                                                                                 |
| ``HTTPS_keyStore_genSelfSigned`` | (optional, if ``HTTPS_active=false`` ) `true`: generate self-signed keystore (only encrypt data, authenticity is not guaranteed). But it is way easier to set up and doesnt really matter if it runs in local network       |
| ``HTTPS_port``                   | (optional, if ``HTTPS_active=false`` ) Port on which the HTTPS server will listen                                                                                                                                           |
| ``HTTP_active``                  | ``true``: run HTTP server on port ``HTTP_port`` (be careful with Basic_Auth credentials)                                                                                                                                    |
| ``HTTP_port``                    | (optional, if ``HTTP_active=false`` ) Port on which the HTTP server will listen                                                                                                                                             |
| ``IPAddress``                    | IP-Address the HTTP or HTTPS server is listening on.<br/>Use ``0.0.0.0`` (or the ip of your device inside the network the request will come from) if the request will not come from your device itself (but e.g. a router). |
| ``LogTime2Comments``             | ``true``: sets the current time as a comment when updating records. Although a ``last-changed`` attribute can be obtained anyway somehow                                                                                    |

Both ``HTTP`` and ``HTTPS`` may be used at the same time if needed.
To use this application with Docker see [here](#docker)


Now you need to configure the device the request is coming from (router, co):
- If ``BasicAuth_active=true``, use your ``BasicAuth_username`` and ``BasicAuth_password``. 
If not, any login credentials may be used or the `Authorization` header field can be completely omitted.
- For ``update-server-address``, use the address under which your pc can be contacted <br>
    (if your pc is in the same network as your router, you can often use the device-name instead of the local ipv4).
You can find your local IPv4 by running ``ipconfig``/``ifconfig``.
- Select the protocol (`HTTP` or `HTTPS`) and port (`HTTP_port` or `HTTPS_port`)
- ``hostname`` is a comma seperated list of different hostnames (if supported by device). <br>
  ``@`` at the beginning of a hostname acts like a wildcard (write `@test.com` instead of ``bla.test.com,test.com,test.test.com``)
  <br> it can be escaped with ``'X'``: ``X@...`` -> ``"@..."``, ``XX@...`` -> ``"X@..."``, ...
  <br> Ex. ``@test.com`` is a wildcard, while `XXX@.test.com` means the concrete domain `"XX@.test.com"` <br>
!EVERYTHING IS CASE-INSENSITIVE!

## Supported Features
Supports Raw HTTP Requests as specified in https://help.dyn.com/remote-access-api/perform-update/. <br>
Returns Return-Codes as specified in https://help.dyn.com/remote-access-api/return-codes/ (or how i understand them).
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
maybe missing some, idk know anymore...


## Releases
### Ver1.0 
Not extensively tested yet but core features should work. If any problems are encountered, please open an Issue with information to replicate it, log entries, config values, ...
#### Fat JAR
Downloadlink: [DyndnsV2_Cloudflare_Proxy.jar](DyndnsV2_Cloudflare_Proxy.jar)
#### JAR with dependencies
Downloadlink: [original-DyndnsV2_Cloudflare_Proxy.jar](original-DyndnsV2_Cloudflare_Proxy.jar)
and
[pom.xml](pom.xml)

#### Docker example files
Downloadlink: [Dockerfile](Dockerfile) and [docker-compose.yml](docker-compose.yml)
