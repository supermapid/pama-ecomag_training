package com.example.supermap;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ZoomControls;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.supermap.data.CoordSysTransMethod;
import com.supermap.data.CoordSysTransParameter;
import com.supermap.data.CursorType;
import com.supermap.data.Dataset;
import com.supermap.data.DatasetType;
import com.supermap.data.DatasetVector;
import com.supermap.data.DatasetVectorInfo;
import com.supermap.data.Datasource;
import com.supermap.data.DatasourceConnectionInfo;
import com.supermap.data.EngineType;
import com.supermap.data.Environment;
import com.supermap.data.GeoStyle;
import com.supermap.data.Geometrist;
import com.supermap.data.Geometry;
import com.supermap.data.Point2D;
import com.supermap.data.PrjCoordSys;
import com.supermap.data.Recordset;
import com.supermap.data.Rectangle2D;
import com.supermap.data.Size2D;
import com.supermap.mapping.Action;
import com.supermap.mapping.ActionChangedListener;
import com.supermap.mapping.GeometryAddedListener;
import com.supermap.mapping.GeometryEvent;
import com.supermap.mapping.GeometryIsSelectedListener;
import com.supermap.mapping.GeometryModifiedListener;
import com.supermap.mapping.GeometryModifyingListener;
import com.supermap.mapping.Layer;
import com.supermap.mapping.LayerSettingVector;
import com.supermap.mapping.Layers;
import com.supermap.mapping.MapControl;
import com.supermap.mapping.MapView;
import com.supermap.mapping.Selection;
import com.supermap.mapping.dyn.DynamicElement;
import com.supermap.mapping.dyn.DynamicPoint;
import com.supermap.mapping.dyn.DynamicStyle;
import com.supermap.mapping.dyn.DynamicText;
import com.supermap.mapping.dyn.DynamicView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import de.hdodenhof.circleimageview.CircleImageView;
import im.delight.android.location.SimpleLocation;



public class MapActivity  extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MapActivity.class.getSimpleName();
    Context mContext;
    Util util;

    SimpleLocation mLocation;
    CircleImageView mBack;

    final String DYN_TAG_TEXT = "DYN_TAG_TEXT_POPUP";
    DynamicText popupText = null;
    String basemap2d;

    // SuperMap Map Objects
    MapControl m_mapcontrol = null;
    MapView m_mapView = null;
    DynamicView mDynamicView = null;
    DynamicPoint mDynGeoPoint = null;
    Layer surveyLayer;
    Geometry currentGeometry = null;
    Datasource surveyDataSource;
    Dataset dtsSurvey;
    Dataset datasetBoundary = null;
    Layer mLayerBoundary = null;
    String datasetBoundaryName = "dsBoundary";
    String layerBoundaryName = "lyrBoundary";

    JSONArray Features_Arr = new JSONArray();
    JSONObject jsonFeatureCollection = new JSONObject();
    JSONObject jsonRoot = new JSONObject();

    ImageButton btnSavePolygon = null;
    ImageButton btnEditPolygon = null;
    ImageButton zoomIn, zoomOut, markerLoc, mLayer;
    ImageButton mUndo = null;
    ImageButton mRedo = null;
    ImageButton mDelete = null;
    FloatingActionButton addButton = null;
    FloatingActionButton doneButton;

    private boolean parcelEdit = false;

    int EPSG = 32748;

    int CODE_FROM_ATTRIBUTE_FORM = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = MapActivity.this;
        util = new Util(getApplicationContext());

        //setting SuperMap license
        try {
            setMapEnvironment();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_map);

        markerLoc = findViewById(R.id.location);
        markerLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLocation(3857, false);
            }
        });


        m_mapView = findViewById(R.id.Map_view);
        m_mapcontrol = m_mapView.getMapControl();

        mDynamicView = new DynamicView(this, m_mapcontrol.getMap());
        m_mapView.addDynamicView(mDynamicView);

        m_mapcontrol.addGeometryAddedListener(new MapAddStatusListener());
        m_mapcontrol.addActionChangedListener( new MapActionChangedLister());
        m_mapcontrol.addGeometryModifiedListener(new MapAddGeometryModifiedListener());
        m_mapcontrol.addGeometryModifyingListener(new MapAddGeometryModifyingListener());
        m_mapcontrol.addGeometryIsSelectedListener(new MapActionisSelectedListener());

        zoomIn = findViewById(R.id.zoomIn);
        zoomOut = findViewById(R.id.zoomOut);
        markerLoc = findViewById(R.id.location);
        markerLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLocation(EPSG, true);
            }
        });

        btnSavePolygon = findViewById(R.id.save);
        btnSavePolygon.setOnClickListener(this);

        btnEditPolygon = findViewById(R.id.edit);
        btnEditPolygon.setOnClickListener( this);

        mUndo = findViewById(R.id.undo);
        mRedo = findViewById(R.id.redo);
        mDelete = findViewById(R.id.cancel);
        addButton = findViewById(R.id.createboundary);
        doneButton = findViewById(R.id.done);

        mUndo.setOnClickListener( this);
        mRedo.setOnClickListener( this);
        mDelete.setOnClickListener( this);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(doneButton.getVisibility() == View.GONE){
                    m_mapcontrol.setAction(Action.CREATEPOLYGON);
                    doneButton.setVisibility(View.VISIBLE);
                    addButton.setVisibility(View.INVISIBLE);
                }else {
                    doneButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext, "Done", Toast.LENGTH_SHORT).show();;
            }
        });

        popupText = new DynamicText();
        popupText.setTag(DYN_TAG_TEXT);

        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_mapcontrol.getMap().zoom(2);
                m_mapcontrol.getMap().refresh();
            }
        });
        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_mapcontrol.getMap().zoom(0.5);
                m_mapcontrol.getMap().refresh();
            }
        });

        setOrtho();

        drawBoundary();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void currentLocation(int UTM_zone, boolean init){
        mLocation = new SimpleLocation(this);
        if (!mLocation.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        final double mLatitude = mLocation.getLatitude();
        final double mLongitude = mLocation.getLongitude();

        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem WGS84 = crsFactory.createFromName("epsg:4326");
        CoordinateReferenceSystem UTM = crsFactory.createFromName("epsg:" + UTM_zone);  //UTM 48S , sesuaikan berdasarkan sitenya.

        Log.e(TAG, "UTM Zone: " + UTM_zone);

        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform wgsToUtm = ctFactory.createTransform(WGS84, UTM);

        // `result` is an output parameter to `transform()`
        ProjCoordinate result = new ProjCoordinate();
        wgsToUtm.transform(new ProjCoordinate(mLongitude, mLatitude), result);


        Point2D pt = new Point2D();
        pt.setX(result.x);
        pt.setY(result.y);

        Log.e(TAG, pt.toString());

        DynamicStyle dynStyle = new DynamicStyle();
        dynStyle.setBackground(BitmapFactory.decodeResource(m_mapcontrol.getResources(), R.drawable.loc)); //png icon

         if (mDynGeoPoint == null) {
            //Draw the central point
            DynamicPoint dynPoint = new DynamicPoint();
            dynPoint.addPoint(pt);

            dynPoint.setStyle(dynStyle);
            mDynGeoPoint = dynPoint;
            mDynamicView.addElement(mDynGeoPoint);
        } else {
            mDynGeoPoint.setPoint(pt);
            mDynGeoPoint.setStyle(dynStyle);
        }

        if(init) m_mapcontrol.getMap().setScale(7500.703471946182E-8);

        m_mapcontrol.panTo(pt, 5);
//        m_mapcontrol.getMap().setCenter(pt);
        //m_mapcontrol.getMap().setViewBounds( geoPoint.getBounds());
        mDynamicView.refresh();
    }

    private void setMapEnvironment() throws IOException {
        String rootPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        rootPath = mContext.getExternalFilesDir(null).getAbsolutePath();

        String mapDir = rootPath + "/SuperMap/";
        String licDir = mapDir + "license/";
        String licFile = licDir + "system.slm";

        String tempDir = mapDir + "temp/";
        String webCacheDir = mapDir + "webcache/";
        String fontDir = mapDir + "fonts/";
        final String dataDir = mapDir + "data/";

        String udb = dataDir + "data.udb";
        String udd = dataDir + "data.udd";

        util.createDirectory(mapDir);
        util.createDirectory(licDir);
        util.createDirectory(tempDir);
        util.createDirectory(webCacheDir);
        util.createDirectory(fontDir);
        util.createDirectory(dataDir);

        Environment.setLicensePath(licDir);
        Environment.setTemporaryPath(tempDir);
        Environment.setWebCacheDirectory(webCacheDir);
        Environment.setFontsPath(fontDir);
        Environment.initialization(mContext);

        int RawLincenseFile = R.raw.license;
        util.copyRAWFileifNotExists(RawLincenseFile, licFile);

        util.copyRAWFileifNotExists2(R.raw.udb,udb);
        util.copyRAWFileifNotExists2(R.raw.udd,udd);
    }

    private void setOSM()
    {
        DatasourceConnectionInfo info = new DatasourceConnectionInfo();
        info.setAlias("OSM");
        info.setEngineType(EngineType.OpenStreetMaps);
        String url = "https://openstreetmap.org";
        info.setServer(url);

        m_mapcontrol.getMap().getLayers().clear();
        boolean isClosed = m_mapcontrol.getMap().getWorkspace().getDatasources().close("OSM");

        Datasource datasource = m_mapcontrol.getMap().getWorkspace().getDatasources().open(info);
        Dataset dtsBasemap = datasource.getDatasets().get(0);
        dtsBasemap.setName("OSM");

        m_mapcontrol = m_mapView.getMapControl();
        m_mapcontrol.getMap().getLayers().add(dtsBasemap, true);

        m_mapcontrol.getMap().refresh();

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(m_mapcontrol.getMap().getLayers().getCount() > 0) {
                    //UTM 48S = EPSG:32748
                    currentLocation(3857, true);
                }
            }
        }, 1000);

        m_mapcontrol.getMap().refresh();
        info.dispose();
    }

    private  void setOrtho()
    {
        String url = "http://ecogear.supermap.id:8090/iserver/services/map-tsbc_2d_ortho_map/rest/maps/tsbc_2d_ortho";

        DatasourceConnectionInfo info = new DatasourceConnectionInfo();
        info.setAlias("ortho");
        info.setEngineType(EngineType.Rest);
        info.setServer(url);

//        m_mapcontrol.getMap().getLayers().clear();

        boolean isClosed = m_mapcontrol.getMap().getWorkspace().getDatasources().close("ortho");

        Datasource datasource = m_mapcontrol.getMap().getWorkspace().getDatasources().open(info);

        Dataset dtsBasemap = datasource.getDatasets().get(0);

        dtsBasemap.setName("ortho");

        m_mapcontrol = m_mapView.getMapControl();

        m_mapcontrol.getMap().getLayers().add(dtsBasemap,true);

        m_mapcontrol.getMap().refresh();

        Rectangle2D rectangle2D = m_mapcontrol.getMap().getLayers().get("tsbc_2d_ortho@ortho").getDataset().getBounds();

        m_mapcontrol.getMap().setViewBounds(rectangle2D);

        m_mapcontrol.getMap().refresh();

    }

    private void drawBoundary() {
        //add layer for drawing boundary

        DatasourceConnectionInfo info2 = new DatasourceConnectionInfo();
        info2.setEngineType( EngineType.VECTORFILE);

        Log.e("EPlan","Start Create memory datasource");
        DatasourceConnectionInfo infoMem = new DatasourceConnectionInfo();
        infoMem.setAlias("boundary");
        infoMem.setEngineType(EngineType.UDB);

        String rootPath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        String UDBPath = rootPath + "/SuperMap/settingfleet.udb";
        infoMem.setServer(UDBPath);

        File file = new File(UDBPath);
        Datasource datasourceMemory = null;

        if (file.exists()){
            datasourceMemory = m_mapcontrol.getMap().getWorkspace().getDatasources().open(infoMem);
        }
        else {
            datasourceMemory = m_mapcontrol.getMap().getWorkspace().getDatasources().create(infoMem);
        }

        DatasetVectorInfo datasetVectorInfo = new DatasetVectorInfo();
        datasetVectorInfo.setName(datasetBoundaryName);
        datasetVectorInfo.setType(DatasetType.REGION);

        if (datasourceMemory.getDatasets().contains(datasetBoundaryName)){
            datasetBoundary = datasourceMemory.getDatasets().get(datasetBoundaryName);
        }
        else{
            datasetBoundary = datasourceMemory.getDatasets().create(datasetVectorInfo);
        }

        datasetBoundary.setPrjCoordSys(m_mapcontrol.getMap().getPrjCoordSys());

        if(m_mapcontrol.getMap().getLayers().contains(layerBoundaryName)){
            mLayerBoundary = m_mapcontrol.getMap().getLayers().get(layerBoundaryName);
        }
        else{
            mLayerBoundary = m_mapcontrol.getMap().getLayers().add(datasetBoundary, true);
        }

        mLayerBoundary.setVisible(true);
        mLayerBoundary.setEditable(true);

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        for (int  i = 0 ; i < m_mapcontrol.getMap().getLayers().getCount() ; i ++){
            Layer lyr = m_mapcontrol.getMap().getLayers().get(i);
            if(lyr != null) {
                lyr.getDataset().close();
            }
        }
        m_mapcontrol.getMap().close();
        m_mapcontrol.getMap().dispose();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.save:
                boolean b =  m_mapcontrol.submit();
                parcelEdit = false;
                mDelete.setImageResource(R.drawable.ic_pencil);
                m_mapcontrol.setAction(Action.PAN);
                break;
            case R.id.undo:
//                try {
//                    readRecordset(datasetBoundary) ;
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
                m_mapcontrol.undo();
                break;
            case R.id.redo:
                m_mapcontrol.redo();
                break;
            case R.id.edit:
                if(!parcelEdit){
                    parcelEdit = true;
                    mLayerBoundary.setEditable(true);
                    m_mapcontrol.setAction(Action.SELECT);
                    btnEditPolygon.setImageResource(R.drawable.ic_cancel_white);

                }else{
                    m_mapcontrol.cancel();
                    m_mapcontrol.setAction(Action.PAN);
                    parcelEdit = false;
                    btnEditPolygon.setImageResource(R.drawable.ic_trash);
                }
                break;
            case R.id.cancel:
//                m_mapcontrol.setAction(Action.VERTEXDELETE);
                if(!parcelEdit){
                    m_mapcontrol.setAction(Action.CREATEPOLYGON);
                    parcelEdit = true;
                    mDelete.setImageResource(R.drawable.ic_cancel_white);
                }else{
                    m_mapcontrol.cancel();
                    m_mapcontrol.setAction(Action.PAN);
                    parcelEdit = false;
                    mDelete.setImageResource(R.drawable.ic_pencil);
                }
                break;
            default:
                break;
        }
    }

    class MapActionChangedLister implements ActionChangedListener {
        @Override
        public void actionChanged(Action action, Action action1) {
            Log.e("EPlan MapActionChagnedLister", action.toString());
        }
    }

    class MapAddStatusListener implements GeometryAddedListener {
        @Override
        public void geometryAdded(GeometryEvent event){
            Geometry addedGeom =  m_mapcontrol.getCurrentGeometry();
            double area = Geometrist.computeGeodesicArea(addedGeom, m_mapcontrol.getMap().getPrjCoordSys());
            String luas = util.convertNumberwithoutDecimal(area,"#,###");
            Log.e(TAG, "Add");
            Toast.makeText(getApplicationContext(), luas.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    class MapAddGeometryModifiedListener implements GeometryModifiedListener {
        @Override
        public void geometryModified(GeometryEvent geometryEvent) {
            Geometry addedGeom =  m_mapcontrol.getCurrentGeometry();
            double area = Geometrist.computeGeodesicArea(addedGeom, m_mapcontrol.getMap().getPrjCoordSys());
            String luas = util.convertNumberwithoutDecimal(area,"#,###");
            Log.e(TAG, "Add Modified");
            Toast.makeText(getApplicationContext(), luas.toString(), Toast.LENGTH_SHORT).show();;
        }
    }

    class MapAddGeometryModifyingListener implements GeometryModifyingListener {
        @Override
        public void geometryModifying(GeometryEvent geometryEvent) {
            Geometry addedGeom =  m_mapcontrol.getCurrentGeometry();
            double area = Geometrist.computeGeodesicArea(addedGeom, m_mapcontrol.getMap().getPrjCoordSys());
            String luas = util.convertNumberwithoutDecimal(area,"#,###");
            Log.e(TAG, "Add Modifying");
            Toast.makeText(getApplicationContext(), luas.toString(), Toast.LENGTH_SHORT).show();;
        }
    }

    class MapActionisSelectedListener implements GeometryIsSelectedListener {
        @Override
        public void geometryIsSelected(boolean b) {
            Log.e("EPlan" , "a geometry has been selected");
            if (b){
                Layer editLayer  = m_mapcontrol.getEditLayer();
                Selection selectedFeature = editLayer.getSelection();
                int selectCount = selectedFeature.getCount();

                Log.e("EPlan" ,"Selected count : " + selectCount);
                editLayer.setEditable(true);
                if(editLayer.isEditable()){
                    Recordset recordset = selectedFeature.toRecordset();
                    recordset.moveFirst();
                    int id = recordset.getID();
                    int[] ids = {id};
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                    builder.setTitle("Delete Polygon");
                    builder.setMessage("Are you sure want to delete this polygon?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            boolean isDeleted = selectedFeature.getDataset().deleteRecords(ids);
                            if (isDeleted){
                                Log.e("EPlan" ,"Success delete a feature with id " + id);
                            }
                            else {
                                Log.e("EPlan" ,"Cannot delete a feature with id " + id);
                            }
                            recordset.dispose();
                            m_mapcontrol.cancel();
                            m_mapcontrol.setAction(Action.PAN);
                            parcelEdit = false;
                            btnEditPolygon.setImageResource(R.drawable.ic_trash);
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            recordset.dispose();
                            m_mapcontrol.cancel();
                            m_mapcontrol.setAction(Action.PAN);
                            parcelEdit = false;
                            btnEditPolygon.setImageResource(R.drawable.ic_trash);
                        }
                    });
                    builder.show();
                }
            }
        }
    }

    private void readRecordset(Dataset dataset) throws JSONException {

        JSONArray jsonArrayBoundaries = new JSONArray();
        JSONObject Boundaries_Obj = new JSONObject();

        JSONArray jsonFeatures = new JSONArray();

        if (dataset != null)
        {
            Recordset recordset = ((DatasetVector)dataset).getRecordset(false, CursorType.STATIC);

            if (recordset.getRecordCount() > 0){
                recordset.moveFirst();

                jsonFeatureCollection.put("type","FeatureCollection");

                JSONArray jsonArrayFeatures = new JSONArray();

                while(!recordset.isEOF())
                {
                    String sFeature = recordset.toGeoJSON(true,1);

                    JSONObject sFeature_Obj = new JSONObject(sFeature);

                    String type_Obj = sFeature_Obj.optString("type");
                    JSONObject geometry_Obj = sFeature_Obj.optJSONObject("geometry");
                    JSONObject properties_Obj = new JSONObject();

                    JSONObject Feature_Obj = new JSONObject();

                    Feature_Obj.put("type", type_Obj);
                    Feature_Obj.put("geometry", geometry_Obj);
                    Feature_Obj.put("properties", properties_Obj);

                    jsonArrayFeatures.put(Feature_Obj);
                    recordset.moveNext();
                }

                jsonFeatureCollection.put("features",jsonArrayFeatures);
                Boundaries_Obj = new JSONObject(jsonFeatureCollection.toString());

                jsonRoot.put("boundaries",Boundaries_Obj);
                Log.e("jsonRoot" , jsonRoot.toString()); // Data yang di kirim

                String fullJsonString = jsonRoot.toString();
                fullJsonString = fullJsonString.replace("[\"", "[");
                fullJsonString = fullJsonString.replace("\"]", "]");
                fullJsonString = fullJsonString.replace("}\",\"{", "},{");

                fullJsonString = fullJsonString.replace("\\\"", "\"");

                Log.e("fullJsonString" , fullJsonString);
            }
            recordset.dispose();
        }
    }
}

