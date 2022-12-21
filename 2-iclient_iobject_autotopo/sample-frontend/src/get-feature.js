import L from "leaflet";
import {
  GetFeaturesBySQLParameters,
  FeatureService,
} from "@supermap/iclient-leaflet";

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

// Get Feature By SQL
// Query dengan SQL, Styling, popup

export function addDesignPit(map) {
  console.log(DATA_URL);
  new FeatureService(DATA_URL).getFeaturesBySQL(
    new GetFeaturesBySQLParameters({
      queryParameter: {
        name: `${dataset}@${datasource}`,
        attributeFilter: "1 = 1",
      },
      toIndex: -1,
      maxFeatures: -1,
      datasetNames: [`${datasource}:${dataset}`],
    }),
    (res) => {
      console.log(res);
      const pit = L.geoJSON(res.result.features, {
        style(feature) {
          console.log(feature.properties.COLORINDEX);
          console.log(COLOR_INDEX[feature.properties.COLORINDEX]);
          return {
            opacity: 1,
            color: COLOR_INDEX[feature.properties.COLORINDEX],
            weight: 5,
          };
        },
        onEachFeature(feature, layer) {
          layer.bindTooltip(
            `<div>Elevasi: ${feature.properties.ELEVATION}</div>`
          );
        },
      });

      console.log(pit);
      pit.addTo(map);
    }
  );
}
