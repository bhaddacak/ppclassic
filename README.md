# Pāli Platform Classic
A classic tool for Pāli studies.

## Building the program

The essential development tools are only *JDK* (11 or newer), *JavaFX* and *Apache Ant*. You can use any IDE/editor to edit the files (but setting up a project in modern IDEs can use some effort). I mainly use Vim/Neovim with a little help from NetBeans for refactoring. If you use a Debian-based GNU/Linux, type this command to install the tools:

```
$ sudo apt-get install openjdk-17-jdk openjfx ant
```

Recently, I have changed my working environment to 32-bit Void Linux. There is no OpenJFX in its repository. So, I use a JDK with JavaFX included instead (Zulu in my case). Also, I use a separate Apache Ant. Once you do it likewise, you have to set JAVA\_HOME and ANT\_HOME and add it to the PATH. I add this to `.bash\_profile` in the following way (replace the directories with yours):

```
export JAVA_HOME=/home/shared/Programs/zulu11.58.17-ca-fx-jdk11.0.16-linux_i686
export ANT_HOME=/home/shared/Programs/apache-ant-1.10.14
export PATH=$PATH:$JAVA_HOME/bin:$ANT_HOME/bin

```

Feel free if you use other distributions of JDK and/or JavaFX (see download guide in the user's manual), but you have to set up your system properly. I suppose you already know it. For other types of OS, you have to tinker by yourselves. I have never built the program in Microsoft Windows nor Apple machines.

The components needed are the following:
- The source files from Github (or by `$ git clone https://github.com/bhaddacak/ppclassic.git`)
- The program's executable package (containing the library and data used)

Once you have all these things, you have to arrange them in this manner:

```
	ROOT-DIR/ (You name it)
	|--.git/
	|--.gitignore
    |--LICENSE
    |--README.md
	|
	|--src/ (the source code)
	|   |--main/
	|   |   |--paliplatform/
	|   |   |    |--*.java
	|   |   |--module-info.java
	|   |--resources/
	|   |   |--fonts/
	|   |   |--images/
	|   |   |--js/
	|   |   |--styles/
	|   |   |--text/
	|   |--build-base.xml
	|   |--build.xml
	|   |--configure
	|   |--manifest.txt
	|
	|--dist/ (containing the final product)
	|   |--PPClassic
	|       |--data/
	|       |--fonts/
	|       |--lib/
	|       |--util/
	|       |--PPClassic.jar
	|       |--some other files...
```

Everything has be to named as shown, and you mostly work in `src` directory. Now, you are ready to build. But you have to set up the build environment first by `./configure`. This will create `build.xml` to be used by Ant. In case you use JDK with JavaFX included and you set up it properly as mentioned above, you may do nothing. But if you have JavaFX/OpenJFX installed separately, you have to update the build script by `./configure -l <jfx-dir>` (see ./configure -h for more information). For example, in typical Debian-based systems the command can be:

```
$ ./configure -l '/usr/share/openjfx/lib'
```

Once everything is put in place, type this command (in `src` directory):

```
$ ant build
```

If you have not edited any source file yet and you set up your system right, you will see `build` directory and `jar/PPClassic.jar` in it. That is all. Then you copy this file to the executable directory, replacing the old one.

If you want to only compile the source code, just type `$ ant compile`. To run the program, type `$ ant run`. For further information, type `$ ant help`. To see all Ant tasks available, type `$ ant -p`.

## Windows executables

Even though the final executable `PPClassic.jar` can be invoked in all platforms using `java` command, to ease Windows users, it is possible to make an exe file out of it. The tool I use is [Launch4j](http://launch4j.sourceforge.net). It is quite simple to use, so I will be brief.

Once you download the program, unpack it somewhere, and open a terminal there. Then, you can run it by this command:

```
$ java -jar launch4j.jar &
```

You should see the program's window now. At the very least, you need to fill in the fields marked by a red asterisk in Basic tab, i.e., the output exe and the executable jar file (both can be browsed by the buttons provided). Then you hit the gear button (Build wrapper) above. That is all. You will see the exe file and you can run it on Windows (JRE is expected to be installed in the system).

For a more refined creation, you can choose an icon file to use. You can also add a splash screen by selecting a bitmap file (BMP, 24-bit without metadata) in Splash tab.

In JRE tab, you can bundle a custom JRE by naming the JRE directory in the program's root directory. In case of 64-bit JRE to be used (the option checked), you need to make a 64-bit exe for it separately.

All these configuration can be saved for future uses. For more information, please read the program's manual.

## Useful links
- [Works of J.R. Bhaddacak](https://bhaddacak.github.io)
- [Pāli Platform's user manual](https://bhaddacak.github.io/ppmanual)

## License
```
Copyright 2023-2024 J.R. Bhaddacak

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/.
```
