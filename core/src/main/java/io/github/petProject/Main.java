package io.github.petProject;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import io.github.petProject.enums.CameraMode;
import io.github.petProject.terrain.HeightMapTerrain;
import io.github.petProject.terrain.Terrain;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.ArrayList;
import java.util.Set;


public class Main extends ApplicationAdapter implements AnimationController.AnimationListener, InputProcessor {

    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene PlayerScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private FirstPersonCameraController cameraController;

    //Player Movement
    float speed = 10f;
    float rotationSpeed = 120f;
    private Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    private final Vector3 currentPosition = new Vector3();


    //Camera
//    private float camHeight = 20f;
    private float camPitch = Settings.CAMERA_START_PITCH;
    private CameraMode cameraMode = CameraMode.FREE_LOOK;
    private float distanceFromPlayer = 10f;
    private float angleAroundPlayer = 0f;
    private float angleBehindPlayer = 0f;
    private boolean anyKeyPressed = false;
    private Music backgroundMusic;
    private Sound stepsSound;


    // Terrain
    private Terrain terrain;
    private Scene terrainScene;


    @Override
    public void create() {

//        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/background.mp3"));
//        backgroundMusic.setLooping(true);
//        backgroundMusic.setVolume(0.1f);
//        backgroundMusic.play();


        stepsSound = Gdx.audio.newSound(Gdx.files.internal("audio/steps.mp3"));
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/Human/Human.gltf"));
        PlayerScene = new Scene(sceneAsset.scene);
        sceneManager = new SceneManager();
        sceneManager.addScene(PlayerScene);
        if (!anyKeyPressed) {
            PlayerScene.animationController.setAnimation("Idle",-1);
        }
        DefaultShader.Config config = new DefaultShader.Config();
        config.numBones = 67;
        sceneManager.setShaderProvider(new DefaultShaderProvider(config));

        PlayerScene.modelInstance.transform.setTranslation(80,0,80);

        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float d = 0.2f;
        camera.near = 1f;
        camera.far = 200;
        sceneManager.setCamera(camera);
        camera.position.set(0, 0,4f);

        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(this);

        cameraController = new FirstPersonCameraController(camera);
//        Gdx.input.setInputProcessor(cameraController);


        // setup light
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));



        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);

//        buildBoxes();
        createTerrain(5f, 0, 0, 0);
        createTerrain(15f, 50 , 0 ,50);
        createTerrain(30f, 25, 0 ,25);
    }

    private void createTerrain(float a, int one, int two, int three) {
        if(terrain != null){
            terrain.dispose();
            sceneManager.removeScene(terrainScene);
        }
        ArrayList<Integer> corners = new ArrayList<Integer>();
        corners.add(one);
        corners.add(two);
        corners.add(three);
        terrain = new HeightMapTerrain(new Pixmap(Gdx.files.internal("textures/heightmap.png")),a,corners);
        terrainScene = new Scene(terrain.getModelInstance());
        sceneManager.addScene(terrainScene);
    }

//    private void buildBoxes() {
//        ModelBuilder modelBuilder = new ModelBuilder();
//        modelBuilder.begin();
//
//        for(int x = 0; x < 100; x += 10){
//            for(int z = 0; z < 100; z += 10){
//                Material material = new Material();
//                material.set(PBRColorAttribute.createBaseColorFactor(Color.RED));
//                MeshPartBuilder builder = modelBuilder.part(x + "," + z, GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, material);
//                BoxShapeBuilder.build(builder, x, 0, z, 1f, 1f, 1f);
//            }
//        }
//
//        ModelInstance model = new ModelInstance(modelBuilder.end());
//        sceneManager.addScene(new Scene(model));
//    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

//      cameraController.update();
//      scene.modelInstance.transform.rotate(Vector3.Y, 10f * deltaTime);

        processInput(deltaTime);
        updateCamera();


        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime);
        sceneManager.render();
    }

    private void updateCamera() {

        float horDistance = calculateHorDistance(distanceFromPlayer);
        float verDistance = calculateVerDistance(distanceFromPlayer);

        calculatePitch();
        calculateAngleAroundPlayer();
        calculateCameraPosition(currentPosition, horDistance, verDistance);


        camera.up.set(Vector3.Y);
//        camera.position.set(currentPosition.x, camHeight, currentPosition.z - camPitch);
        camera.lookAt(currentPosition);
        camera.update();
    }

    private void calculateCameraPosition(Vector3 currentPosition, float horDistance, float verDistance) {
          float offsetX = (float) (horDistance * Math.sin(Math.toRadians(angleAroundPlayer)));
          float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(angleAroundPlayer)));

          camera.position.x = currentPosition.x - offsetX;
          camera.position.z = currentPosition.z - offsetZ;
          camera.position.y = currentPosition.y + verDistance;
    }

    private void calculateAngleAroundPlayer() {
        if (cameraMode == CameraMode.FREE_LOOK){
            float angleChange = Gdx.input.getDeltaX() * Settings.CAMERA_ANGLE_AROUND_PLAYER_FACTOR;
            angleAroundPlayer -= angleChange;
        } else {
            angleAroundPlayer = angleBehindPlayer;
        }
    }

    private void calculatePitch() {
        float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
        camPitch -= pitchChange;

        if (camPitch <Settings.CAMERA_MIN_PITCH){
            camPitch = Settings.CAMERA_MIN_PITCH;
        } else if(camPitch > Settings.CAMERA_MAX_PITCH){
            camPitch = Settings.CAMERA_MAX_PITCH;
        }
    }

    private float calculateVerDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateHorDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
    }




    private void processInput(float deltaTime) {
        //Update player transform
        if (!anyKeyPressed && !PlayerScene.animationController.current.animation.id.equals("RunJump")) {
            PlayerScene.animationController.setAnimation("Idle", -1);
        }
        playerTransform.set(PlayerScene.modelInstance.transform);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
            Gdx.app.exit();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE) && Gdx.input.isKeyPressed(Input.Keys.W)){
            PlayerScene.animationController.action("RunJump",1,0.9f,this,0.2f);
            moveTranslation.z += speed * deltaTime;
            anyKeyPressed = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)){
            PlayerScene.animationController.action("RunJump",1,0.9f,this,0.2f);
            anyKeyPressed = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) && !Gdx.input.isKeyPressed(Input.Keys.SPACE)){
            PlayerScene.animationController.animate("Running", 1, this,0.5f);
            moveTranslation.z += speed * deltaTime;
            anyKeyPressed = true; }
        else if (!PlayerScene.animationController.current.animation.id.equals("RunJump")){
            PlayerScene.animationController.setAnimation("Idle", -1);
            anyKeyPressed = false;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.S)){
            moveTranslation.z -= speed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)){
            playerTransform.rotate(Vector3.Y,rotationSpeed * deltaTime);
            angleBehindPlayer += rotationSpeed * deltaTime;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)){
            playerTransform.rotate(Vector3.Y,-rotationSpeed * deltaTime);
            angleBehindPlayer -= rotationSpeed * deltaTime;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
            switch (cameraMode) {
                case FREE_LOOK:
                    cameraMode = CameraMode.BEHIND_PLAYER;
                    angleAroundPlayer = angleBehindPlayer;
                    break;
                case BEHIND_PLAYER:
                    cameraMode = CameraMode.FREE_LOOK;
                    break;
            }
        }


        playerTransform.translate(moveTranslation);

        // Set the modified transform
        PlayerScene.modelInstance.transform.set(playerTransform);

        // Update vector position
        PlayerScene.modelInstance.transform.getTranslation(currentPosition);

        float height = terrain.getHeightAtWorldCoord(currentPosition.x, currentPosition.z);

        currentPosition.y = height;

        PlayerScene.modelInstance.transform.setTranslation(currentPosition);

        // Clear the move translation out
        moveTranslation.set(0,0,0);
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        diffuseCubemap.dispose();
    }

    @Override
    public void onEnd(AnimationController.AnimationDesc animation) {

    }

    @Override
    public void onLoop(AnimationController.AnimationDesc animation) {

    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float zoomLevel = amountY * Settings.CAMERA_ZOOM_LEVEL_FACTOR;
        distanceFromPlayer += zoomLevel;
        if(distanceFromPlayer < Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER){
            distanceFromPlayer = Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER;
        }
        return false;
    }
}
