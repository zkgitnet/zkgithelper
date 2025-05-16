# ZK Git Helper
## General 
This lightweight component integrates with the standard Git software and automatically activates each time a Git command is executed. It intercepts traffic from the Git native protocol, enabling manipulation of Git communication and acting as an intermediary between the Git software and the ZK Git Client.

## Install
Create a file named 'git-remote-zkgit' in the directory */usr/bin/*, with the following content:

```bash
#!/bin/bash
java -jar /path/to/zkgithelper.jar "$@" "$(pwd)" | tee /dev/tty
```
