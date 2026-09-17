package com.alfa.device_ctrl;

/** Immutable typed projection of one installable Linux runtime from the canonical registry. */
public final class RuntimeProfile {
    private final String id;
    private final String displayName;
    private final String version;
    private final String architecture;
    private final String rootfsUrl;
    private final String rootfsSha256;
    private final String archiveFormat;
    private final String shell;
    private final String packageManager;
    private final String promptContract;
    private final String[] environment;
    private final String[] requiredPaths;
    private final String[] capabilities;
    private final boolean rootfsGzip;
    private final String[] prootArguments;

    public RuntimeProfile(String id,String displayName,String version,String architecture,String rootfsUrl,String rootfsSha256,boolean rootfsGzip,String archiveFormat,String shell,String packageManager,String promptContract,String[] environment,String[] requiredPaths,String[] capabilities,String[] prootArguments) {
        this.id=require(id,"id");this.displayName=require(displayName,"displayName");this.version=require(version,"version");this.architecture=require(architecture,"architecture");this.rootfsUrl=require(rootfsUrl,"rootfsUrl");this.rootfsSha256=require(rootfsSha256,"rootfsSha256");this.rootfsGzip=rootfsGzip;this.archiveFormat=require(archiveFormat,"archiveFormat");this.shell=require(shell,"shell");this.packageManager=require(packageManager,"packageManager");this.promptContract=require(promptContract,"promptContract");this.environment=copy(environment,"environment");this.requiredPaths=copy(requiredPaths,"requiredPaths");this.capabilities=copy(capabilities,"capabilities");this.prootArguments=copy(prootArguments,"prootArguments");
    }
    public String id(){return id;} public String displayName(){return displayName;} public String version(){return version;} public String architecture(){return architecture;} public String rootfsUrl(){return rootfsUrl;} public String rootfsSha256(){return rootfsSha256;} public boolean rootfsGzip(){return rootfsGzip;} public String archiveFormat(){return archiveFormat;} public String shell(){return shell;} public String packageManager(){return packageManager;} public String promptContract(){return promptContract;} public String[] environment(){return environment.clone();} public String[] requiredPaths(){return requiredPaths.clone();} public String[] capabilities(){return capabilities.clone();} public String[] prootArguments(){return prootArguments.clone();}
    private static String require(String value,String name){if(value==null||value.trim().isEmpty()||value.indexOf('\0')>=0)throw new IllegalArgumentException(name+" is invalid");return value;}
    private static String[] copy(String[] values,String name){if(values==null||values.length==0)throw new IllegalArgumentException(name+" is empty");String[] result=values.clone();for(String value:result)require(value,name);return result;}
}
