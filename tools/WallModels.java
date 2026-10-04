import java.io.File;
import java.util.Arrays;
import net.runelite.cache.IndexType;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Store;

/** Offline metadata inspection only; never packaged in the plugin. */
public class WallModels
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 2) { throw new IllegalArgumentException("WallModels CACHE_DIRECTORY OBJECT_ID..."); }
		try (Store store = new Store(new File(args[0])))
		{
			store.load();
			ObjectManager objects = new ObjectManager(store);
			objects.load();
			for (int i = 1; i < args.length; i++)
			{
				int id = Integer.parseInt(args[i]);
				ObjectDefinition object = objects.getObject(id);
				if (object == null) { System.out.println("Missing object " + id); continue; }
				System.out.println("OBJECT " + id + " name=" + object.getName() + " animation=" + object.getAnimationID()
					+ " footprint=" + object.getSizeX() + "x" + object.getSizeY()
					+ " scale=" + object.getModelSizeX() + "," + object.getModelSizeHeight() + "," + object.getModelSizeY()
					+ " models=" + Arrays.toString(object.getObjectModels())
					+ " types=" + Arrays.toString(object.getObjectTypes()) + " ambient=" + object.getAmbient()
					+ " contrast=" + object.getContrast() + " rotated=" + object.isRotated()
					+ " recolour=" + Arrays.toString(object.getRecolorToFind()) + " -> " + Arrays.toString(object.getRecolorToReplace())
					+ " retexture=" + Arrays.toString(object.getRetextureToFind()) + " -> " + Arrays.toString(object.getTextureToReplace()));
				if (object.getObjectModels() == null) { continue; }
				for (int modelId : object.getObjectModels())
				{
					Archive archive = store.getIndex(IndexType.MODELS).getArchive(modelId);
					byte[] packed = archive == null ? null : store.getStorage().loadArchive(archive);
					if (packed == null) { System.out.println("Missing model " + modelId); continue; }
					byte[] data = archive.getFiles(packed).getFiles().iterator().next().getContents();
					ModelDefinition model = new ModelLoader().load(modelId, data);
					System.out.println("MODEL " + modelId + " vertices=" + model.vertexCount + " faces=" + model.faceCount
						+ " bounds=" + bounds(model.vertexX) + "," + bounds(model.vertexY) + "," + bounds(model.vertexZ)
						+ " texturedFaces=" + texturedFaces(model.faceTextures));
				}
			}
		}
	}

	private static int texturedFaces(short[] textures)
	{
		int count = 0;
		if (textures != null) { for (short texture : textures) { if (texture != -1) { count++; } } }
		return count;
	}

	private static String bounds(int[] vertices)
	{
		return Arrays.stream(vertices).min().orElse(0) + ".." + Arrays.stream(vertices).max().orElse(0);
	}
}
