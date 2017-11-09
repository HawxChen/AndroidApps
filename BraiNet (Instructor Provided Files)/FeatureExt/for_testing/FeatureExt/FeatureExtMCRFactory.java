/*
 * MATLAB Compiler: 5.1 (R2014a)
 * Date: Wed Apr 06 12:49:06 2016
 * Arguments: "-B" "macro_default" "-W" "java:FeatureExt,Class1" "-T" "link:lib" "-d" 
 * "C:\\Users\\aagraw25\\Documents\\MATLAB\\FeatureExt\\for_testing" "-v" 
 * "C:\\Users\\aagraw25\\workspace\\MatlabConnection\\MatlabConnection\\FeatureExt.m" 
 * "class{Class1:C:\\Users\\aagraw25\\workspace\\MatlabConnection\\MatlabConnection\\FeatureExt.m}" 
 */

package FeatureExt;

import com.mathworks.toolbox.javabuilder.*;
import com.mathworks.toolbox.javabuilder.internal.*;

/**
 * <i>INTERNAL USE ONLY</i>
 */
public class FeatureExtMCRFactory
{
   
    
    /** Component's uuid */
    private static final String sComponentId = "FeatureExt_D546E712DB36B915E2DEFBBCE47CEE41";
    
    /** Component name */
    private static final String sComponentName = "FeatureExt";
    
   
    /** Pointer to default component options */
    private static final MWComponentOptions sDefaultComponentOptions = 
        new MWComponentOptions(
            MWCtfExtractLocation.EXTRACT_TO_CACHE, 
            new MWCtfClassLoaderSource(FeatureExtMCRFactory.class)
        );
    
    
    private FeatureExtMCRFactory()
    {
        // Never called.
    }
    
    public static MWMCR newInstance(MWComponentOptions componentOptions) throws MWException
    {
        if (null == componentOptions.getCtfSource()) {
            componentOptions = new MWComponentOptions(componentOptions);
            componentOptions.setCtfSource(sDefaultComponentOptions.getCtfSource());
        }
        return MWMCR.newInstance(
            componentOptions, 
            FeatureExtMCRFactory.class, 
            sComponentName, 
            sComponentId,
            new int[]{8,3,0}
        );
    }
    
    public static MWMCR newInstance() throws MWException
    {
        return newInstance(sDefaultComponentOptions);
    }
}
