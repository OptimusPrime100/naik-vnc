# Naik VNC Privacy Policy

**Effective date:** 9 September 2026
**App:** Naik VNC
**Developer:** Naik_Labs
**Package:** `com.naiklabs.dexvnc`

Naik VNC is an independent open-source VNC client derived from bVNC. This Privacy Policy explains how Naik VNC accesses, uses, stores, and transmits information when you use the app.

## 1. Summary

Naik_Labs does not operate a backend service that collects your Naik VNC connection profiles or remote-desktop session contents.

Naik VNC primarily stores configuration locally on your device and communicates directly with servers, computers, gateways, and infrastructure that you choose to connect to.

The current Naik VNC release does not intentionally include advertising SDKs, third-party analytics SDKs, crash-reporting SDKs, or a Naik_Labs telemetry service.

## 2. Information stored locally on your device

Naik VNC can store connection profiles and preferences locally so that you can reconnect without entering the same settings each time.

Depending on the features and configuration you use, locally stored information can include:

- server hostname or IP address;
- port numbers;
- usernames;
- connection names and display preferences;
- VNC connection settings;
- SSH gateway or tunneling settings;
- certificates, public keys, private-key-related configuration, or other authentication material that you provide;
- passwords or other credentials when you choose an option that saves them;
- mouse, scaling, input, and Samsung DeX-related preferences.

These settings are used to provide the functions you request and are not uploaded to a Naik_Labs server.

Some application data may be stored in a local database that uses SQLCipher components. Naik VNC does not represent that every locally stored credential is independently encrypted in every configuration. Device security, screen locking, operating-system protections, and your own credential-handling choices remain important.

## 3. Network connections and remote-desktop data

Naik VNC is a network client. When you connect to a remote system, the app transmits information needed to establish and operate that connection directly to the destination you selected.

Depending on the features you use, this can include communications with:

- VNC servers;
- SSH servers or gateways;
- Proxmox or oVirt infrastructure;
- other user-configured remote systems supported by the underlying project.

Remote-desktop session data can include screen updates, keyboard input, mouse input, clipboard-related data where a feature is enabled, authentication information, and protocol metadata required for the connection.

Naik_Labs does not act as an intermediary for these sessions and does not receive a copy of your remote-desktop traffic through a Naik_Labs service.

The confidentiality of a remote session depends on the protocol, server, tunnel, encryption, authentication, and network configuration that you choose. You are responsible for using servers and security settings that you trust.

## 4. Local network discovery

Where local-network discovery functionality is used, Naik VNC may inspect or probe addresses and service ports on your local network to locate compatible systems.

Discovery information is used for app functionality and is not sent to a Naik_Labs backend.

## 5. Permissions and device access

The current Naik VNC release requests permissions used for core functionality, including:

- **Internet access** — to connect to remote systems that you select;
- **Network state** — to determine network connectivity;
- **Vibration** — for supported user-interface feedback.

The current release does not request runtime permissions for location, contacts, SMS, phone calls, camera, general file storage, or microphone access.

## 6. Analytics, advertising, and telemetry

The current Naik VNC release does not intentionally include:

- advertising SDKs;
- third-party analytics SDKs;
- third-party crash-reporting SDKs;
- a Naik_Labs telemetry or tracking backend.

The Google Play Store, Android operating system, device manufacturer, network provider, or services that you independently connect to may process information under their own privacy policies and terms. Those services are not operated by Naik_Labs.

## 7. Sharing of information

Naik_Labs does not sell your personal information.

Naik_Labs does not receive or share your saved Naik VNC connection profiles through a Naik_Labs backend because no such backend is used for those profiles.

Information is transmitted to remote systems when you direct Naik VNC to connect to them. Those transmissions are necessary to provide the remote-access functionality you requested. The operator of a server you connect to may receive and process information according to that operator's own policies and configuration.

## 8. Data retention and deletion

Connection profiles and preferences stored by Naik VNC remain on your device until they are changed or removed, the app's local storage is cleared, or the app is uninstalled.

You can remove saved connection information from the app where the relevant management option is available. You can also use Android's app-storage controls or uninstall Naik VNC to remove locally stored app data.

Naik_Labs does not maintain a server-side Naik VNC account database or a server-side copy of your connection profiles that requires a separate account-deletion request.

Data stored or logged by a remote server that you chose to connect to is controlled by that server's operator and is outside Naik_Labs' control.

## 9. Security

Naik VNC uses Android application storage and the security mechanisms provided by the underlying protocols and libraries used by the app.

No method of storing or transmitting information can be guaranteed to be completely secure. Users should protect their devices, use trusted networks and servers, avoid saving credentials when unnecessary, and use encrypted tunnels or secure protocol options where appropriate.

Naik VNC does not intentionally log plaintext or decrypted credential material through the credential encryption/decryption diagnostic logging that was present in the upstream code and removed in this fork.

## 10. Accounts

Naik VNC does not currently provide a Naik_Labs user-account system.

Because there is no Naik_Labs account system, there is no separate Naik_Labs cloud account or associated cloud profile to delete.

## 11. Open-source software and third-party components

Naik VNC is an independent modified fork of bVNC and uses open-source libraries and components.

Source code and licensing information are available at:

https://github.com/OptimusPrime100/naik-vnc

Third-party components and external services may have their own licenses, privacy practices, and security characteristics.

Naik VNC is not affiliated with or endorsed by the original bVNC developer or Samsung Electronics.

## 12. Changes to this Privacy Policy

This Privacy Policy may be updated when Naik VNC's features, data practices, or legal and platform requirements change.

The effective date at the top of this page will be updated when material changes are made.

## 13. Contact

For privacy questions about Naik VNC, use the Naik VNC GitHub Issues page:

https://github.com/OptimusPrime100/naik-vnc/issues

**Do not include passwords, private keys, authentication tokens, or other sensitive credentials in a public GitHub issue.**

Developer: **Naik_Labs**
