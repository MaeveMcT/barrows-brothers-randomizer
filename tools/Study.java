import java.io.*;
import java.util.*;
import com.google.gson.*;
import net.runelite.cache.*;
import net.runelite.cache.fs.*;
import net.runelite.cache.region.*;
import net.runelite.cache.definitions.*;
import net.runelite.cache.definitions.loaders.ModelLoader;

public class Study {
 public static void main(String[] args) throws Exception {
  JsonObject keys=JsonParser.parseReader(new FileReader(args[1])).getAsJsonObject();
  try(Store store=new Store(new File(args[0]))) {
   store.load();
   ObjectManager objects=new ObjectManager(store); objects.load();
   UnderlayManager underlays=new UnderlayManager(store); underlays.load();
   OverlayManager overlays=new OverlayManager(store); overlays.load();
   RegionLoader loader=new RegionLoader(store, id -> {
    JsonElement k=keys.get(""+id); if(k==null)return new int[4];
    int[] v=new int[4];for(int i=0;i<4;i++)v[i]=k.getAsJsonArray().get(i).getAsInt();return v;
   });
   List<Object> report=new ArrayList<>();
   for(String arg:args[2].split(",")) {
    int regionId=Integer.parseInt(arg);Map<String,Object> r=new LinkedHashMap<>(); r.put("region",regionId);
    Region region;
    try { region=loader.loadRegionFromArchive(regionId); }catch(Exception e){r.put("error",e.toString()); report.add(r);continue;}
    if(region==null){r.put("error","unavailable map"); report.add(r);continue;}
    List<Object> floors=new ArrayList<>();
    for(int z=0;z<4;z++) {
     Map<Integer,Integer> us=new TreeMap<>(),os=new TreeMap<>();
     for(int x=0;x<64;x++)for(int y=0;y<64;y++) {
      int u=region.getUnderlayId(z,x,y),o=region.getOverlayId(z,x,y);
      if(u>0)us.merge(u-1,1,Integer::sum); if(o>0)os.merge(o-1,1,Integer::sum);
     }
     Map<String,Object> f=new LinkedHashMap<>();f.put("plane",z);
     List<Object> ul=new ArrayList<>(),ol=new ArrayList<>();
     for(int id:us.keySet()){UnderlayDefinition d=underlays.getUnderlays().stream().filter(v->v.getId()==id).findFirst().orElse(null);if(d!=null)ul.add(Map.of("id",id,"count",us.get(id),"rgb",d.getColor()));}
     for(int id:os.keySet()){OverlayDefinition d=overlays.provide(id);if(d!=null)ol.add(Map.of("id",id,"count",os.get(id),"rgb",d.getRgbColor(),"texture",d.getTexture()));}
     f.put("underlays",ul);f.put("overlays",ol);floors.add(f);
    }
    r.put("floors",floors);
    Map<Integer,List<Location>> locs=new TreeMap<>();for(Location l:region.getLocations())locs.computeIfAbsent(l.getId(),v->new ArrayList<>()).add(l);
    List<Object> props=new ArrayList<>();
    for(int id:locs.keySet()) {
     ObjectDefinition d=objects.getObject(id);if(d==null)continue;
     Map<String,Object> p=new LinkedHashMap<>();p.put("id",id);p.put("name",d.getName());p.put("count",locs.get(id).size());
     p.put("planes",locs.get(id).stream().map(l->l.getPosition().getZ()).distinct().sorted().toArray());
     p.put("types",locs.get(id).stream().map(Location::getType).distinct().sorted().toArray());
     p.put("animation",d.getAnimationID());p.put("morphs",d.getConfigChangeDest());p.put("models",d.getObjectModels());
     Map<Integer,Integer> ts=new TreeMap<>(),cs=new TreeMap<>();
     List<Integer> missing=new ArrayList<>();
     if(d.getObjectModels()!=null)for(int m:d.getObjectModels()) {
      Archive a=store.getIndex(IndexType.MODELS).getArchive(m); if(a==null){missing.add(m);continue;}
      byte[] packed=store.getStorage().loadArchive(a);if(packed==null){missing.add(m);continue;}
      try {
       ModelDefinition md=new ModelLoader().load(m,a.getFiles(packed).getFiles().iterator().next().getContents());
       if(md.faceTextures!=null)for(short t:md.faceTextures){int v=t;if(d.getRetextureToFind()!=null)for(int i=0;i<d.getRetextureToFind().length;i++)if(v==d.getRetextureToFind()[i]){v=d.getTextureToReplace()[i];break;}if(v>=0)ts.merge(v,1,Integer::sum);}
       if(md.faceColors!=null)for(short c:md.faceColors){int v=c&65535;if(d.getRecolorToFind()!=null)for(int i=0;i<d.getRecolorToFind().length;i++)if(v==(d.getRecolorToFind()[i]&65535)){v=d.getRecolorToReplace()[i]&65535;break;}cs.merge(v,1,Integer::sum);}
      }catch(Exception e){missing.add(m);}
     }
     p.put("textures",ts);p.put("colours",cs);p.put("missingModels",missing);props.add(p);
    }
    r.put("objects",props);report.add(r);
   }
   System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(report));
  }
 }
}
