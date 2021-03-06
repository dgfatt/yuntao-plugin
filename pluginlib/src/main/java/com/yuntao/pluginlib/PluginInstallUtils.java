package com.yuntao.pluginlib;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import dalvik.system.DexClassLoader;

/**
 * Created by pengyuntao on 16-6-29.
 */
public class PluginInstallUtils {

    private String dexOutputPath;
    private Context mContext;
    private static PluginInstallUtils sInstance;
    public final static HashMap<String, PluginEnv> mPackagesHolder = new HashMap<String, PluginEnv>();

    private PluginInstallUtils(Context context) {
        mContext = context.getApplicationContext();
    }

    public static PluginInstallUtils getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PluginInstallUtils.class) {
                if (sInstance == null) {
                    sInstance = new PluginInstallUtils(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 安装插件环境
     *
     * @param apkPath
     * @return
     */
    public PluginEnv installRunEnv(String apkPath) {
        PluginEnv pluginPackage = mPackagesHolder.get(apkPath);
        if (pluginPackage != null) {
            return pluginPackage;
        }
        String unZipPath = apkPath.replace(".apk", "/");
        String name = apkPath.substring(apkPath.lastIndexOf("/"), apkPath.length()).replace(".apk", "");
        FileUtils.unZip(apkPath, unZipPath);//解压插件apk
//        File dir = getDir("lib", Context.MODE_PRIVATE);
        String nativeLibPath = "/data/data/com.yuntao.host/pluginLib" + name;
        try {
            FileUtils.copyFolder(unZipPath + "lib/armeabi", nativeLibPath);//测试copy插件的so到唯一单独文件夹
            FileUtils.deleteFile(nativeLibPath + "/libnativeCore.so");//测试删除主程序的so
        } catch (IOException e) {
            e.printStackTrace();
        }
        DexClassLoader dexClassLoader = createDexClassLoader(apkPath, nativeLibPath);
        AssetManager assetManager = createAssetManager(apkPath);
        Resources resources = createResources(assetManager);
        Resources.Theme theme = resources.newTheme();
        theme.applyStyle(R.style.AppTheme, false);
        // create pluginPackage
        pluginPackage = new PluginEnv(apkPath, dexClassLoader, resources, assetManager, theme);
        mPackagesHolder.put(apkPath, pluginPackage);
        return pluginPackage;
    }

    /**
     * 创建插件classloader
     * @param dexPath
     * @return
     */
    private DexClassLoader createDexClassLoader(String dexPath, String nativeLibPath) {
        File dexOutputDir = mContext.getDir("dex", Context.MODE_PRIVATE);
        dexOutputPath = dexOutputDir.getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(dexPath, dexOutputPath, nativeLibPath, mContext.getClassLoader());
        return loader;
    }

    /**
     * 创建AssetManager对象
     *
     * @param dexPath
     * @return
     */
    private AssetManager createAssetManager(String dexPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            return assetManager;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建Resource对象
     *
     * @param assetManager
     * @return
     */
    private Resources createResources(AssetManager assetManager) {
        Resources superRes = mContext.getResources();
        Resources resources = new Resources(assetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
        return resources;
    }

    private Class<?> loadPluginClass(ClassLoader classLoader, String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return clazz;
    }
}
