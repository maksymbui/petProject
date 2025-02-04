package io.github.petProject.terrain;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.mygdx.game.terrains.HeightField;
import io.github.petProject.Main;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

import java.util.ArrayList;
import java.util.List;

public class HeightMapTerrain extends Terrain{

    private static final Vector3 c00 = new Vector3();
    private static final Vector3 c01 = new Vector3();
    private static final Vector3 c10 = new Vector3();
    private static final Vector3 c11 = new Vector3();

    private HeightField field;

    public HeightMapTerrain(Pixmap data, float magnitude, ArrayList<Integer> corners){
        this.size = 200;
        this.width = data.getWidth();
        this.heightMagnitude = magnitude;

        field = new HeightField(true, data, true, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        data.dispose();
        field.corner00.set(0, 0, 0);
        field.corner10.set(size, 0, 0);
        field.corner01.set(0, 0, size);
        field.corner11.set(size, 0, size);
        field.magnitude.set(0f, magnitude, 0f);
        field.update();

        Texture texture = new Texture(Gdx.files.internal("textures/sand-dunes1_albedo.png"),true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        PBRTextureAttribute textureAttribute = PBRTextureAttribute.createBaseColorTexture(texture);
        textureAttribute.scaleU = 50f;
        textureAttribute.scaleV = 50f;

        Material material = new Material();
        material.set(textureAttribute);

        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("terrain", field.mesh, GL20.GL_TRIANGLES, material);
        modelInstance = new ModelInstance(mb.end());

        modelInstance.transform.setTranslation(corners.get(0), corners.get(1), corners.get(2));
    }

    @Override
    public void dispose(){
        field.dispose();
    }

    @Override
    public float getHeightAtWorldCoord(float worldX, float worldZ) {
        modelInstance.transform.getTranslation(c00);
        float terrainX = worldX - c00.x;
        float terrainZ = worldZ - c00.z;

        float gridSquareSize = size / ((float) width - 1);

        int gridX = (int) Math.floor(terrainX / gridSquareSize);
        int gridZ = (int) Math.floor(terrainZ / gridSquareSize);

        if(gridX >= width - 1 || gridZ >= width -1 || gridX < 0 || gridZ < 0) {
            return 0;
        }

        float xCoord = ((terrainX % gridSquareSize) / gridSquareSize);
        float zCoord = ((terrainZ % gridSquareSize) / gridSquareSize);

        float height;
        if (xCoord <= (1 - zCoord)){
            height = barryCentric(
                c00.set(0,field.data[gridZ * width + gridX], 0),
                c10.set(1, field.data[gridZ * width + (gridX + 1)], 0),
                c01.set(0, field.data[(gridZ + 1) * width + gridX], 1),
                new Vector2(xCoord, zCoord));
        } else {
            height = barryCentric(
                c10.set(1,field.data[gridZ * width + (gridX + 1)], 0),
                c11.set(1, field.data[(gridZ + 1) * width + (gridX + 1)], 1),
                c01.set(0, field.data[(gridZ + 1) * width + gridX], 1),
                new Vector2(xCoord, zCoord));        }

        return height * heightMagnitude;
    }

    public static float barryCentric(Vector3 p1, Vector3 p2, Vector3 p3, Vector2 pos) {
        float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
        float l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
        float l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
        float l3 = 1.0f - l1 - l2;
        return l1 * p1.y + l2 * p2.y + l3 * p3.y;

    }
}
