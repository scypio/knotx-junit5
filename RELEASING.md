# Signing
In order to create OpenPGP signatures, you will need a key pair (instructions on creating a key pair using the GnuPG tools can be found in the GnuPG HOWTOs). 
You need to provide the Signing Plugin with your key information, which means three things:

The public key ID (The last 8 symbols of the keyId. You can use gpg -K to get it).
The absolute path to the secret key ring file containing your private key. 
(Since gpg 2.1, you need to export the keys with command gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg).

The passphrase used to protect your private key.

These items must be supplied as the values of the signing.keyId, signing.secretKeyRingFile, and signing.password properties, respectively.

Given the personal and private nature of these values, a good practice is to store them in the gradle.properties file in the user’s Gradle home directory.

```
signing.keyId=24875D73
signing.password=secret
signing.secretKeyRingFile=/Users/me/.gnupg/secring.gpg
```

# Credentials
Additionally you need to configure your **ossrh** credentials to deploy artifacts to Maven Central.
```
ossrhUsername=username
ossrhPassword=secret
```

# Uploading to nexus
`./gradlew publish`

- is it was snapshot version it will go to snapshot repo
- otherwise to nexus staging
