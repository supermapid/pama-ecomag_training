async function parseExtent(url) {
  const res = await fetch(`${url}/config`);

  const config = await res.text();
  const left = parseFloat(
    config.substring(
      config.indexOf("<sml:Left>") + "<sml:Left>".length,
      config.indexOf("</sml:Left>")
    )
  );
  const right = parseFloat(
    config.substring(
      config.indexOf("<sml:Right>") + "<sml:Right>".length,
      config.indexOf("</sml:Right>")
    )
  );
  const top = parseFloat(
    config.substring(
      config.indexOf("<sml:Top>") + "<sml:Top>".length,
      config.indexOf("</sml:Top>")
    )
  );
  const bottom = parseFloat(
    config.substring(
      config.indexOf("<sml:Bottom>") + "<sml:Bottom>".length,
      config.indexOf("</sml:Bottom>")
    )
  );
  const x = (left + right) / 2;
  const y = (top + bottom) / 2;
  let z = (right - left) * 111319.49079327358 * 2;
  if (z < 0) {
    z = 3000;
  }

  return [x, y, z];
}

// Get Feature By SQL
// Query dengan SQL, Styling, popup

const DATA_URL = "http://localhost:8090/iserver/services/data-ecomag/rest/data";
const datasource = "mtbu_data";
const dataset = "T2112_Desain_Pit_MTBUL";

const COLOR_INDEX = {
  1: "#FF0000",
  3: "#FFC349",
  4: "#86E929",
  5: "#00A0E9",
  7: "#E36C09",
  30: "#D33249",
};

const PROJ_48S =
  "+proj=utm +zone=48 +south +datum=WGS84 +units=m +no_defs +type=crs";
const PROJ_4326 = "+proj=longlat +datum=WGS84 +no_defs";
function addDesignPit(map) {
  console.log(DATA_URL);
  L.supermap.featureService(DATA_URL).getFeaturesBySQL(
    new SuperMap.GetFeaturesBySQLParameters({
      queryParameter: {
        name: `${dataset}@${datasource}`,
        attributeFilter: "1 = 1",
      },
      toIndex: -1,
      maxFeatures: -1,
      datasetNames: [`${datasource}:${dataset}`],
    }),
    (res) => {
      for (const feature of res.result.features.features) {
        const elevation = parseFloat(feature.properties.ELEVATION);

        let coordinates;

        if (feature.geometry.type === "LineString") {
          coordinates = feature.geometry.coordinates.flatMap((c) => {
            return [...proj4(PROJ_48S, PROJ_4326, c), elevation];
          });
        } else {
          coordinates = feature.geometry.coordinates.flat().flatMap((c) => {
            return [...proj4(PROJ_48S, PROJ_4326, c), elevation];
          });
        }

        map.entities.add({
          description:
            '<table class="table table-striped table-condensed">' +
            '<tr style="padding: 0.1rem"><th style="padding: 0.1rem">ID </th><td style="padding: 0.1rem"> : ' +
            feature.properties.ENTITYHANDLE +
            "</td></tr>" +
            '<tr style="padding: 0.1rem"><th style="padding: 0.1rem">Date </th><td style="padding: 0.1rem"> : ' +
            feature.properties.GPS_TIME +
            "</td></tr>" +
            '<tr style="padding: 0.1rem"><th style="padding: 0.1rem">Elevation </th><td style="padding: 0.1rem"> : ' +
            feature.properties.ELEVATION +
            "</td></tr>" +
            "</table>",
          polyline: {
            positions: Cesium.Cartesian3.fromDegreesArrayHeights(coordinates),
            width: 4,
            material: Cesium.Color.fromCssColorString(
              COLOR_INDEX[feature.properties.COLORINDEX]
            ),
          },
        });
      }
    }
  );
}

let viewer;
const basemap3dUrl =
  "http://localhost:8090/iserver/services/3D-mtbu_3d_utm_all/rest/realspace/datas/T2112_Ortho_Pit_MTBU__UTM48S_@mtbu_data";

const demUrl =
  "http://localhost:8090/iserver/services/3D-BM_3D_MTBU_220510/rest/realspace/datas/T220321_DEM_MTBU@scene";

async function setupMap() {
  const [x, y, z] = await parseExtent(basemap3dUrl);

  viewer = new Cesium.Viewer("map", {
    requestRenderMode: true,
    maximumRenderTimeChange: Infinity,
    targetFrameRate: 60,
  });

  viewer.scene.terrainProvider = new Cesium.CesiumTerrainProvider({
    url: demUrl,
    invisibility: true,
  });

  viewer.imageryLayers.addImageryProvider(
    new Cesium.SuperMapImageryProvider({
      url: basemap3dUrl,
    })
  );

  viewer.camera.setView({
    destination: new Cesium.Cartesian3.fromDegrees(x, y, z),
  });

  addDesignPit(viewer);
}

setupMap();
