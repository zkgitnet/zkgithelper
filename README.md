# ZK Git Helper
## General 
This lightweight component integrates with the standard Git software and automatically activates each time a Git command is executed. It intercepts traffic from the Git native protocol, enabling manipulation of Git communication and acting as an intermediary between the Git software and the ZK Git Client.

## Install
Create a file named 'git-remote-zkgit' in the directory */usr/bin/*, with the following content:

```bash
#!/bin/bash
java -jar /path/to/zkgithelper.jar "$@" "$(pwd)" | tee /dev/tty
```

## Repo Configuration
To configure your repository remote, use the following URI format:
```zkgit::git@localhost:10101/<reponame>.git```
Replace `<reponame>` with the actual name of your repository.

For a repository named **TestRepo1**, add the remote using the following command:
```git remote add zkgitremote zkgit::git@localhost:10101/TestRepo1.git```


## Git Usage
Once the remote is configured as shown above, you can use Git commands (`push`, `pull`, `clone`, etc.) as usual. All encryption and decryption is handled automatically by the ZK Git Client.

**Important:**
The ZK Git Client must be running in the background and the user must be logged in for Git operations to succeed.
