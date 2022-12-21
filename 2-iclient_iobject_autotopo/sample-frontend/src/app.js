import L from "leaflet";
import { tiledMapLayer } from "@supermap/iclient-leaflet";
import { addDesignPit } from "./get-feature";
import { createProjection } from "./sm-helper";

// 1. Menambahkan basemap pihak ketiga
// const map = L.map("map");
// map.setView([-7, 110], 13);
// const basemapUrl = `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}${
//   L.Browser.retina ? "@2x" : ""
// }.png`;

// L.tileLayer(basemapUrl).addTo(map);

// 2. Menambahkan basemap iServer UTM 48S
// 2.a. Membuat proyeksi sesuai dengan basemap

const basemapUtmUrl =
  "http://localhost:8090/iserver/services/map-ecomag/rest/maps/mtbu_2d_utm_ortho";

// createProjection(basemapUtmUrl);

// 2.b. Menambahkan basemap 48S
let map;
let crs;
async function setupUtmMap() {
  crs = await createProjection(basemapUtmUrl);
  const info = await getBasemapInfo(basemapUtmUrl);

  map = L.map("map", {
    crs,
    center: [info.center.y, info.center.x],
    maxZoom: 22,
    zoom: 10,
    renderer: L.canvas(),
  });

  const tiled = tiledMapLayer(basemapUtmUrl);

  addDesignPit(map);

  tiled.addTo(map);
}

setupUtmMap();
