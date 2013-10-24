/*******************************************************************************
 * Copyright 2013 Esri
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.esri.squadleader.view;

import java.io.FileNotFoundException;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import com.esri.android.map.MapView;
import com.esri.militaryapps.controller.LocationController.LocationMode;
import com.esri.militaryapps.model.LayerInfo;
import com.esri.squadleader.R;
import com.esri.squadleader.controller.AdvancedSymbologyController;
import com.esri.squadleader.controller.MapController;
import com.esri.squadleader.model.BasemapLayer;
import com.esri.squadleader.view.AddLayerFromWebDialogFragment.AddLayerListener;
import com.esri.squadleader.view.GoToMgrsDialogFragment.GoToMgrsHelper;

/**
 * The main activity for the Squad Leader application. Typically this displays a map with various other
 * controls.
 */
public class SquadLeaderActivity extends FragmentActivity
        implements AddLayerListener, GoToMgrsHelper {
    
    private static final String TAG = SquadLeaderActivity.class.getSimpleName();
    
    private MapController mapController = null;
    private AdvancedSymbologyController mil2525cController = null;
    private AddLayerFromWebDialogFragment addLayerFromWebDialogFragment = null;
    private GoToMgrsDialogFragment goToMgrsDialogFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        MapView mapView = (MapView) findViewById(R.id.map);
        mapController = new MapController(mapView, getAssets());
        ((NorthArrowView) findViewById(R.id.northArrowView)).setMapController(mapController);
        try {
            mil2525cController = new AdvancedSymbologyController(
                    mapController,
                    getAssets(),
                    getString(R.string.sym_dict_dirname));
            mapController.setAdvancedSymbologyController(mil2525cController);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't find file while loading AdvancedSymbologyController", e);
        }
    }
    
    public MapController getMapController() {
        return mapController;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapController.pause();
    }
    
    @Override
    protected void onResume() {
        super.onResume(); 
        mapController.unpause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_layer_from_web:
                //Present Add Layer from Web dialog
                if (null == addLayerFromWebDialogFragment) {
                    addLayerFromWebDialogFragment = new AddLayerFromWebDialogFragment();
                }
                addLayerFromWebDialogFragment.show(getSupportFragmentManager(), getString(R.string.add_layer_from_web_fragment_tag));
                return true;
            case R.id.go_to_mgrs:
                //Present Go to MGRS dialog
                if (null == goToMgrsDialogFragment) {
                    goToMgrsDialogFragment = new GoToMgrsDialogFragment();
                }
                goToMgrsDialogFragment.show(getSupportFragmentManager(), getString(R.string.go_to_mgrs_fragment_tag));
                return true;
            case R.id.set_location_mode:
                //Present Set Location Mode dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.set_location_mode)
                        .setNegativeButton(R.string.cancel, null)
                        .setSingleChoiceItems(
                                new String[] {
                                        getString(R.string.option_location_service),
                                        getString(R.string.option_simulation_builtin),
                                        getString(R.string.option_simulation_file)},
                                mapController.getLocationController().getMode() == LocationMode.LOCATION_SERVICE ? 0 : 
                                    null == mapController.getLocationController().getGpxFile() ? 1 : 2,
                                new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (2 == which) {
                                        //TODO present file chooser
                                    } else {
                                        mapController.getLocationController().setMode(
                                                0 == which ? LocationMode.LOCATION_SERVICE : LocationMode.SIMULATOR,
                                                true);
                                    }
                                } catch (Exception e) {
                                    Log.d(TAG, "Couldn't set location mode", e);
                                } finally {
                                    dialog.dismiss();
                                }
                            }
                            
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public void imageButton_zoomIn_clicked(View view) {
	mapController.zoomIn();
    }
    
    public void imageButton_zoomOut_clicked(View view) {
	mapController.zoomOut();
    }
    
    public void imageButton_openBasemapPanel_clicked(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_basemap)
                .setNegativeButton(R.string.cancel, null);
        List<BasemapLayer> basemapLayers = mapController.getBasemapLayers();
        String[] basemapLayerNames = new String[basemapLayers.size()];
        for (int i = 0; i < basemapLayers.size(); i++) {
            basemapLayerNames[i] = basemapLayers.get(i).getLayer().getName();
        }
        builder.setSingleChoiceItems(
                basemapLayerNames,
                mapController.getVisibleBasemapLayerIndex(),
                new DialogInterface.OnClickListener() {
            
            public void onClick(DialogInterface dialog, int which) {
                mapController.setVisibleBasemapLayerIndex(which);
                dialog.dismiss();
            }
            
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    public void imageButton_openMapMenu_clicked(final View view) {
        openOptionsMenu();
    }

    public void onValidLayerInfos(LayerInfo[] layerInfos) {
        for (int i = layerInfos.length - 1; i >= 0; i--) {
            mapController.addLayer(layerInfos[i]);
        }
    }
    
    public void toggleButton_grid_clicked(final View view) {
        mapController.setGridVisible(((ToggleButton) view).isChecked());
    }
    
    public void northArrowView_clicked(View view) {
        mapController.setRotation(0);
    }

    public void toggleButton_followMe_clicked(final View view) {
        mapController.setAutoPan(((ToggleButton) view).isChecked());
    }

}