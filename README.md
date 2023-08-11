# Lambda locator
A tool that can be used to find lambdas in java applications. It can also be used to disassemble jar files, apk files,
and it is also usable as a library for use within projects making use of proguard-core. It is able to open class pools
and disassemble classes within these class pools. It allows changes to be made to Clazz objects without making new
instances of these Clazz objects which make it possible to edit Clazz objects while working on them in a ProGuard pass
for example. Making changes like this only works using a custom build of proguard-assembler, this build is currently
not included in this repository.