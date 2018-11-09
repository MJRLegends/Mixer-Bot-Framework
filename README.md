# Mixer-Bot-Framework
A Mixer Bot Framework made in Java using the Mixer API. Made for making the creation of Bot's for Mixer quicker


#### Current Version: 1.0.0
### With Maven
In your `pom.xml` add:
```xml
<repositories>
  <repository>
    <id>mjrlegends-maven</id>
    <url>https://maven.mjrlegends.com/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.mjr.mjrmixer</groupId>
    <artifactId>Mixer-Bot-Framework</artifactId>
    <version>@VERSION@</version>
  </dependency>
</dependencies>
```
### With Gradle
In your `build.gradle` add: 
```groovy
repositories {
  	maven {
	    name 'MJRLegends'
	    url = "https://maven.mjrlegends.com/"
    }
}

dependencies {
  compile "com.mjr.mjrmixer:Mixer-Bot-Framework:@VERSION@"
}
```

<a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-nd/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-nd/4.0/">Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International License</a>. **For more information on the license see** https://tldrlegal.com/license/creative-commons-attribution-noncommercial-noderivs-(cc-nc-nd)#summary
