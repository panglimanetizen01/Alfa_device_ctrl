package com.alfa.device_ctrl;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Typed projection of the canonical runtime/runtimes.v1.json registry. */
public final class RuntimeRegistry {
    private static final String REGISTRY_ASSET = "runtimes.v1.json";
    private static final String SCHEMA_VERSION = "runtime-registry.v1";
    private static final String ENGINE_CONTRACT = "multi-distro-linux-runtime.v1";
    private static final String TARGET_ARCHITECTURE = "arm64-v8a";

    private static volatile RegistryData cached;

    private RuntimeRegistry() { }

    public static List<RuntimeProfile> all(){return load().profiles;}

    public static RuntimeProfile get(String id){
        if(id==null)return null;
        for(RuntimeProfile profile:load().profiles)if(profile.id().equals(id))return profile;
        return null;
    }

    public static String canonicalRegistrySha256(){return load().sha256;}

    static List<RuntimeProfile> parseCanonicalJson(String json){
        try {
            JSONObject registry=new JSONObject(json);
            requireEquals(registry.getString("schema_version"),SCHEMA_VERSION,"schema_version");
            requireEquals(registry.getString("engine_contract"),ENGINE_CONTRACT,"engine_contract");
            requireEquals(registry.getString("target_architecture"),TARGET_ARCHITECTURE,"target_architecture");
            JSONArray runtimes=registry.getJSONArray("runtimes");
            List<RuntimeProfile> profiles=new ArrayList<>(runtimes.length());
            for(int i=0;i<runtimes.length();i++)profiles.add(parseProfile(runtimes.getJSONObject(i)));
            if(profiles.isEmpty())throw new IllegalArgumentException("runtimes is empty");
            return Collections.unmodifiableList(profiles);
        } catch(Exception error) {
            if(error instanceof IllegalArgumentException)throw (IllegalArgumentException)error;
            throw new IllegalArgumentException("invalid canonical runtime registry",error);
        }
    }

    private static RuntimeProfile parseProfile(JSONObject runtime){
        String acceptanceStatus=runtime.getString("acceptance_status");
        int acceptanceOrder=runtime.getInt("acceptance_order");
        if(!"SUPPORTED".equals(acceptanceStatus)||acceptanceOrder<1)throw new IllegalArgumentException("runtime acceptance metadata is invalid");
        return new RuntimeProfile(
                runtime.getString("runtime_id"),
                runtime.getString("family"),
                runtime.getString("version"),
                runtime.getString("architecture"),
                runtime.getString("rootfs_uri"),
                runtime.getString("rootfs_sha256"),
                runtime.getBoolean("rootfs_gzip"),
                runtime.getString("archive_format"),
                runtime.getString("shell_path"),
                runtime.getString("package_manager"),
                runtime.getString("prompt_contract"),
                stringArray(runtime.getJSONArray("environment")),
                stringArray(runtime.getJSONArray("required_paths")),
                stringArray(runtime.getJSONArray("capabilities")),
                stringArray(runtime.getJSONArray("proot_arguments")));
    }

    private static String[] stringArray(JSONArray values){
        String[] result=new String[values.length()];
        if(result.length==0)throw new IllegalArgumentException("registry array is empty");
        for(int i=0;i<values.length();i++)result[i]=values.getString(i);
        return result;
    }

    private static void requireEquals(String actual,String expected,String field){
        if(!expected.equals(actual))throw new IllegalArgumentException(field+" mismatch: "+actual);
    }

    private static RegistryData load(){
        RegistryData current=cached;
        if(current!=null)return current;
        synchronized(RuntimeRegistry.class){
            current=cached;
            if(current==null){
                byte[] bytes=readCanonicalRegistry();
                List<RuntimeProfile> profiles=parseCanonicalJson(new String(bytes,StandardCharsets.UTF_8));
                current=new RegistryData(profiles,sha256(bytes));
                cached=current;
            }
            return current;
        }
    }

    private static byte[] readCanonicalRegistry(){
        try {
            Context context=AlfaApplication.getInstance();
            if(context!=null)return read(context.getAssets().open(REGISTRY_ASSET));
            File file=new File("runtime/runtimes.v1.json");
            if(!file.isFile())file=new File("../runtime/runtimes.v1.json");
            if(!file.isFile())throw new IllegalStateException("canonical runtime registry is missing");
            return read(new FileInputStream(file));
        } catch(Exception error) {
            throw new IllegalStateException("cannot load canonical runtime registry",error);
        }
    }

    private static byte[] read(InputStream input)throws Exception{
        try(InputStream stream=input;ByteArrayOutputStream output=new ByteArrayOutputStream()){
            byte[] buffer=new byte[4096];
            int count;
            while((count=stream.read(buffer))!=-1)output.write(buffer,0,count);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] bytes){
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result=new StringBuilder(digest.length*2);
            for(byte value:digest)result.append(String.format("%02x",value));
            return result.toString();
        } catch(Exception error) {
            throw new IllegalStateException("SHA-256 unavailable",error);
        }
    }

    private static final class RegistryData {
        final List<RuntimeProfile> profiles;
        final String sha256;
        RegistryData(List<RuntimeProfile> profiles,String sha256){this.profiles=profiles;this.sha256=sha256;}
    }
}
