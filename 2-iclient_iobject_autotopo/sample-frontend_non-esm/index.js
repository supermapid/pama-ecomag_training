// 1. Menambahkan basemap pihak ketiga
// const map = L.map("map");
// map.setView([-7, 110], 13);
// const basemapUrl = `https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}${
//   L.Browser.retina ? "@2x" : ""
// }.png`;

// L.tileLayer(basemapUrl).addTo(map);

// 2. Menambahkan basemap iServer UTM 48S
// 2.a. Membuat proyeksi sesuai dengan basemap
function getStyleResolutions(bounds) {
  let styleResolutions = [];
  let temp = Math.abs(bounds.left - bounds.right) / 256;
  for (let i = 0; i < 22; i++) {
    if (i == 0) {
      styleResolutions[i] = temp;
      continue;
    }
    temp = temp / 2;
    styleResolutions[i] = temp;
  }
  return styleResolutions;
}

const basemapUtmUrl =
  "http://128.199.133.7:8091/iserver/services/map-pama_mtbu/rest/maps/T2112_Ortho_Pit_MTBU_UTM48S";

async function getBasemapExtent(url) {
  const res = await fetch(`${url}/prjCoordSys/projection/extent.json`);
  return await res.json();
}

async function getBasemapProjInfo(url) {
  const res = await fetch(`${url}/prjCoordSys.json`);
  return await res.json();
}

async function getBasemapProjWkt(url) {
  const res = await fetch(`${url}/prjCoordSys.wkt`);
  const file = await res.blob();
  return await file.text();
}

async function getBasemapInfo(url) {
  const res = await fetch(`${url}.json`);
  return await res.json();
}

async function createProjection(url) {
  const bounds = await getBasemapExtent(url);
  const projDef = await getBasemapProjWkt(url);
  const projInfo = await getBasemapProjInfo(url);

  const resolutions = getStyleResolutions(bounds);
  proj4.defs(projInfo.name, projDef);

  return new L.CRS.NonEarthCRS({
    bounds: L.bounds([bounds.left, bounds.bottom], [bounds.right, bounds.top]),
    resolutions,
    origin: L.point(bounds.left, bounds.top),
  });
}

// createProjection(basemapUtmUrl);

// 2.b. Menambahkan basemap 48S
let map;
let crs;
async function setupUtmMap() {
  crs = await createProjection(basemapUtmUrl);
  const info = await getBasemapInfo(basemapUtmUrl);

  console.log(crs);
  console.log(info);

  map = L.map("map", {
    crs,
    center: [info.center.y, info.center.x],
    maxZoom: 22,
    zoom: 10,
    renderer: L.canvas(),
  });

  const tiled = L.supermap.tiledMapLayer(basemapUtmUrl);
  tiled.addTo(map);
}

setupUtmMap();
