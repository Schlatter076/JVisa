************************************** Projects ********************************

There are two NetBeans 8.2 projects, JVisa and JVisaOscilloscope which compile
into libraries in the "dist" folder, JVisa.jar and JVisaOscilloscope.jar 
respectively. In the ant script in JVisa/build.xml I added combining JVisa.jar 
and JnaVisa64_13.jar into JnaVisaOneJar.jar and copying the latter into the 
JVisaOscilloscope/lib folder.
Use JVisaOscilloscope for VISA compliant oscilloscopes (putting JVisaOneJar.jar
and JVisaOscilloscope.jar in the classpath) and JVisa for all other VISA 
compliant instruments (putting JVisaOneJar.jar in the classpath). You have to 
download a VISA library into a classpath folder since I cannot distribute it 
here for copyright reasons.


*********************************** Configurations for 64bit *******************

Previous versions came in three configurations, 32-bit Windows, 64-bit Windows,
and 64-bit Linux (which was not working until now). I decided to drop support
for 32-bit Windows and managed to have only one JVisa configuration (64bit) for
both, 64-bit Windows and 64-bit Linux, tested under Windows 10 and Rebecca Mint
respectively. I still kept two configurations for JVisa (default which is not
working and 64bit) to allow adding configurations for future development.
JVisaOscilloscope comes only in a default configuration that uses 
JnaVisaOneJar.jar which contains JVisa and all necessary JNA libraries.
To achieve loading different libraries, information of this thread  was 
used: https://netbeans.org/bugzilla/show_bug.cgi?id=148590
I added a 64bit property file to nbproject.configs: 64bit.properties. It 
contains a reference to the respective library folder (64bit)
and is referenced by nbproject.project.properties.run.classpath.
To build the 64bit configuration it has to be selected in the NetBeans IDE
project configuration pull-down list.


***************** Unsigned Types ***********************************************

Java 8 can treat objects of type Integer and Long as unsigned. Should I
make use of it in the get and set functions for attributes?


********************************* Testing **************************************

I decided to use TestNG and not JUnit. There are three suites in Test Packages,
JVisaSuiteWindows.xml, JVisaSuiteLinux.xml, and JVisaSuiteNoInstrument.xml. You
can use the latter to test communication (by means of function calls) with the 
native VISA library. The former ones you can use to also test communication with
any VISA compliant instrument. There are two different suites for Windows and
Linux because the Linux native library libvisa.so (http://www.librevisa.org)
does not implement the VISA API 5.0 in its entirety. For example, as attribute
for an instrument session only VI_ATTR_RSRC_SPEC_VERSION is implemented.
For JVisaOscilloscope I wrote the suite JVisaOscilloscopeSuite.xml. You have to
modify it if you want to run the test with an oscilloscope different from the
Tektronix TDS3000 series. I am prepared to write a driver if you lend me an
oscilloscope :) .

All classes derived from jvisa use one logger (java.util.logging) that gets
instantiated in the super class (jvisa). By default its level is set to SEVERE.
Since it is declared as public you can change its settings. You can for instance
remove the file handler to disable file logging.


***************** Issues ***********************************************

Sometimes (I would say less than 10%), the (native?) library fails to open the 
instrument. Under Windows you can just try again, or increase the timeout if 
that error persists. Under Linux, the libreVisa library hangs the application 
until it times out after many seconds and throws an exception which JVisa does 
not catch (yet) since this exception is not part of the VISA API. When I faced 
this issue in my tests I just killed the job and tried again.
