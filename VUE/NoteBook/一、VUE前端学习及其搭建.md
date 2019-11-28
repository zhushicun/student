# 一、VUE前端学习及其搭建

## 1.安装node.js

## 2.安装vue

## 3.生成一个vue脚手架网站

### 3.1安装vue-cli

#### 3.1.1安装CNPM

​       下面整个过程是基于已经安装node.js和cnpm的基础上，node.js如何安装就不在这里详说了。如何全局化安装cnpm，这里简单提一下：

```
npm install cnpm -g --registry=https://registry.npm.taobao.org
```

​        其实对于安装vue-cli，使用npm命令和cnpm命令都是可以的，个人觉得使用npm安装的比较慢，而且很可能会因为网络问题而出错，所以还是觉得使用cnpm稳一点。

#### 3.1.2全局安装vue-cli

```
cnpm install -g vue-cli
```

### 3.2选定文件夹创建网站

​        安装vue-cli成功后，通过cd命令进入你想放置项目的文件夹，在命令提示窗口执行创建vue-cli工程项目的命令：

vue init webpack
![显示的文字](D:/学习笔记/VUE/picture/创建工程效果.png"创建工程图片")
各个属性按照英文翻译就行，其中ESLint是一种语言规范，比较严格。
确认创建项目后，后续还需输入一下项目名称、项目描述、作者、打包方式、是否使用ESLint规范代码等等，详见上图。

### 3.3cnpm安装依赖

cnpm install 

### 3.4最后需要执行命令： npm run dev 来启动项目，启动完成后会自动弹出默认网页：

npm run dev

构建完毕！

