package codechicken.obfuscator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;

import codechicken.core.asm.ObfMapping;

public class ObfuscationMap
{
    public class ObfuscationEntry
    {
        public final ObfMapping obf;
        public final ObfMapping srg;
        public final ObfMapping mcp;
        
        public ObfuscationEntry(ObfMapping obf, ObfMapping srg, ObfMapping mcp)
        {
            this.obf = obf;
            this.srg = srg;
            this.mcp = mcp;
        }
    }
    
    private class ClassEntry extends ObfuscationEntry
    {
        public Map<String, ObfuscationEntry> mcpMap = new HashMap<String, ObfuscationEntry>();
        public Map<String, ObfuscationEntry> srgMap = new HashMap<String, ObfuscationEntry>();
        public Map<String, ObfuscationEntry> obfMap = new HashMap<String, ObfuscationEntry>();
        
        public ClassEntry(String obf, String srg)
        {
            super(new ObfMapping(obf, "", ""), 
                    new ObfMapping(srg, "", ""), 
                    new ObfMapping(srg, "", ""));
        }
        
        public ObfuscationEntry addEntry(ObfMapping obf_desc, ObfMapping srg_desc)
        {
            ObfuscationEntry entry = new ObfuscationEntry(obf_desc, srg_desc, srg_desc.copy());
            obfMap.put(obf_desc.s_name.concat(obf_desc.s_desc), entry);
            srgMap.put(srg_desc.s_name, entry);
            srgMemberMap.put(srg_desc.s_name, entry);
            return entry;
        }

        public void inheritFrom(ClassEntry p)
        {
            obfMap.putAll(p.obfMap);
            srgMap.putAll(p.srgMap);
            mcpMap.putAll(p.mcpMap);
        }
    }
    
    private Map<String, ClassEntry> srgMap = new HashMap<String, ClassEntry>();
    private Map<String, ClassEntry> obfMap = new HashMap<String, ClassEntry>();
    private ArrayListMultimap<String, ObfuscationEntry> srgMemberMap = ArrayListMultimap.create();
    private ObfuscationRun run;
    public ObfuscationMap(ObfuscationRun run)
    {
        this.run = run;
    }
    
    public ObfuscationEntry addClass(String obf, String srg)
    {
        return addEntry(new ObfMapping(obf, "", ""), new ObfMapping(srg, "", ""));
    }
    
    public ObfuscationEntry addField(String obf_owner, String obf_name, String srg_owner, String srg_name)
    {
        return addEntry(new ObfMapping(obf_owner, obf_name, ""), 
                new ObfMapping(srg_owner, srg_name, ""));
    }
    
    public ObfuscationEntry addMethod(String obf_owner, String obf_name, String obf_desc, String srg_owner, String srg_name, String srg_desc)
    {
        return addEntry(new ObfMapping(obf_owner, obf_name, obf_desc), 
                new ObfMapping(srg_owner, srg_name, srg_desc));
    }
    
    public ObfuscationEntry addEntry(ObfMapping obf_desc, ObfMapping srg_desc)
    {
        ClassEntry e = srgMap.get(srg_desc.s_owner);
        if(e == null)
        {
            e = new ClassEntry(obf_desc.s_owner, srg_desc.s_owner);
            obfMap.put(obf_desc.s_owner, e);
            srgMap.put(srg_desc.s_owner, e);
        }
        if(obf_desc.s_name.length() > 0)
            return e.addEntry(obf_desc, srg_desc);
        
        return e;
    }
    
    public void addMcpName(String srg_name, String mcp_name)
    {
        List<ObfuscationEntry> entries = srgMemberMap.get(srg_name);
        if(entries.isEmpty())
            run.err().println("Tried to add mcp name ("+mcp_name+") for unknown srg key ("+srg_name+")");
        else
        {
            for(ObfuscationEntry entry : entries)
            {
                entry.mcp.s_name = mcp_name;
                srgMap.get(entry.srg.s_owner).mcpMap.put(entry.mcp.s_name.concat(entry.mcp.s_desc), entry);
            }
        }
    }
    
    public ObfuscationEntry lookupSrg(String srg_key)
    {
        List<ObfuscationEntry> list = srgMemberMap.get(srg_key);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public ObfuscationEntry lookupMcpClass(String name)
    {
        return srgMap.get(name);
    }

    public ObfuscationEntry lookupObfClass(String name)
    {
        return obfMap.get(name);
    }

    public ObfuscationEntry lookupMcpField(String owner, String name)
    {
        return lookupMcpMethod(owner, name, "");
    }

    public ObfuscationEntry lookupObfField(String owner, String name)
    {
        return lookupObfMethod(owner, name, "");
    }

    public ObfuscationEntry lookupMcpMethod(String owner, String name, String desc)
    {
        run.buildSuperMap(owner);
        ClassEntry e = srgMap.get(owner);
        return e == null ? null : e.mcpMap.get(name.concat(desc));
    }

    public ObfuscationEntry lookupObfMethod(String owner, String name, String desc)
    {
        run.buildSuperMap(owner);
        ClassEntry e = obfMap.get(owner);
        return e == null ? null : e.obfMap.get(name.concat(desc));
    }

    public void inherit(String name, String parent)
    {
        ClassEntry e = srgMap.get(name);
        if(e == null)
            throw new IllegalStateException("Tried to inerit to an undefined class: "+name+" extends "+parent);
        ClassEntry p = srgMap.get(parent);
        if(p == null)
            throw new IllegalStateException("Tried to inerit from undefired parent: "+name+" extends "+parent);
        e.inheritFrom(p);
    }
}
