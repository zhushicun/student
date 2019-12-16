# [linux下docker如何指定容器的工作目录?](https://www.cnblogs.com/dakewei/p/11010554.html)



答: 启动容器时传入-w <work_dir>参数即可,如:

　　docker run -it -w <work_dir> <container_image_name> <command>

　　示例:

　　docker run -it -w /home/jello centos /bin/bash

　　

　　参数解析:

　　-i: 交互

　　-t: 分配一个伪终端

　　-w: 指定工作目录