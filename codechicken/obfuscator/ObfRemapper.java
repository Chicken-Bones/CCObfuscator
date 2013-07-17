package codechicken.obfuscator;

import org.objectweb.asm.commons.Remapper;

import codechicken.lib.asm.ObfMapping;
import codechicken.obfuscator.ObfuscationMap.ObfuscationEntry;

public class ObfRemapper extends Remapper
{
    private ObfuscationRun run;
    public ObfRemapper(ObfuscationRun run)
    {
        this.run = run;
    }
    
    @Override
    public String map(String name)
    {
        ObfuscationEntry map;
        if(run.obfuscate)
            map = run.obf.lookupMcpClass(name);
        else
            map = run.obf.lookupObfClass(name);

        if(map != null)
            return run.obfuscate(map).s_owner;
        
        return name;
    }
    
    @Override
    public String mapFieldName(String owner, String name, String desc)
    {
        ObfuscationEntry map;
        if(run.obfuscate)
            map = run.obf.lookupMcpField(owner, name);
        else
            map = run.obf.lookupObfField(owner, name);
        
        if(map == null)
            map = run.obf.lookupSrg(name);
        
        if(map != null)
            return run.obfuscate(map).s_name;
        
        return name;
    }
    
    @Override
    public String mapMethodName(String owner, String name, String desc)
    {
        if(owner.charAt(0) == '[')
            return name;
        
        ObfuscationEntry map;
        if(run.obfuscate)
            map = run.obf.lookupMcpMethod(owner, name, desc);
        else
            map = run.obf.lookupObfMethod(owner, name, desc);
        
        if(map == null)
            map = run.obf.lookupSrg(name);
        
        if(map != null)
            return run.obfuscate(map).s_name;
        
        return name;
    }

    public void map(ObfMapping map)
    {
        if(map.s_desc.contains("("))
        {
            map.s_name = mapMethodName(map.s_owner, map.s_name, map.s_desc);
            map.s_desc = mapMethodDesc(map.s_desc);
        }
        else
        {
            map.s_name = mapFieldName(map.s_owner, map.s_name, map.s_desc);
            map.s_desc = mapDesc(map.s_desc);
        }
        map.s_owner = map(map.s_owner);
    }
    
    @Override
    public Object mapValue(Object cst)
    {
        if(!(cst instanceof String))
            return cst;

        if(run.srg_cst)
        {
            ObfuscationEntry map = run.obf.lookupSrg((String) cst);
            if(map != null)
                return run.obfuscate(map).s_name;
        }
        return cst;
    }
}
