# uca - The Microscopic Certificate Authority
Uca (mu-c-a) is a tiny wrapper script around OpenSSL for managing certificates.

## Usage
1.  Setup a new authority: creates a new root certificate and serial counter.
        ```./uca setup```

2.  Issue a new certificate: create a new certificate and sign it with the root certificate.
        ```./uca issue <name>```

Run `uca` without any options for help.