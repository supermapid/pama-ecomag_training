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
import com.supermap.data.Workspace;
import com.supermap.data.WorkspaceConnectionInfo;
import com.supermap.data.WorkspaceType;
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

import im.delight.android.location.SimpleLocation;



public class MapActivity  extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();
    Context mContext;
    Util util;

    SimpleLocation mLocation;

    // SuperMap Map Objects
    MapControl m_mapcontrol = null;
    MapView m_mapView = null;
    DynamicView mDynamicView = null;
    DynamicPoint mDynGeoPoint = null;

    ImageButton zoomIn, zoomOut, markerLoc;

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
                currentLocation(4326, false);
            }
        });


        m_mapView = findViewById(R.id.Map_view);
        m_mapcontrol = m_mapView.getMapControl();

        mDynamicView = new DynamicView(this, m_mapcontrol.getMap());
        m_mapView.addDynamicView(mDynamicView);


        zoomIn = findViewById(R.id.zoomIn);
        zoomOut = findViewById(R.id.zoomOut);
        markerLoc = findViewById(R.id.location);
        markerLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLocation(4326, true);
            }
        });

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

        openWorkspace();
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

        Environment.setLicensePath(licDir);
        Environment.setTemporaryPath(tempDir);
        Environment.setWebCacheDirectory(webCacheDir);
        Environment.setFontsPath(fontDir);
        Environment.initialization(mContext);

    }
    protected  void openWorkspace()
    {
        String rootPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        rootPath = mContext.getExternalFilesDir(null).getAbsolutePath();

        //Open workspace
        Workspace m_workspace = new Workspace();
        WorkspaceConnectionInfo info = new WorkspaceConnectionInfo();
        info.setServer(rootPath + "/SuperMap/data/GeometryInfo/World.smwu");
        info.setType(WorkspaceType.SMWU);
        m_workspace.open(info);

        //Connect map display controller with workspace
        m_mapView = (MapView)findViewById(R.id.Map_view);
        m_mapcontrol = m_mapView.getMapControl();
        m_mapcontrol.getMap().setWorkspace(m_workspace);

        //Open 2nd map in workspace
        String mapName = m_workspace.getMaps().get(0);
        m_mapcontrol.getMap().open(mapName);

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
}

