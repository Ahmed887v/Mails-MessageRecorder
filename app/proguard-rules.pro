# JavaMail / activation keep rules
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class java.beans.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn java.beans.**

# AppAuth
-keep class net.openid.appauth.** { *; }
