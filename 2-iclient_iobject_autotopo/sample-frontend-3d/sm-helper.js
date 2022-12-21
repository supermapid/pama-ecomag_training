export function getStyleResolutions(bounds) {
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

export async function getBasemapExtent(url) {
  const res = await fetch(`${url}/prjCoordSys/projection/extent.json`);
  return await res.json();
}

export async function getBasemapProjInfo(url) {
  const res = await fetch(`${url}/prjCoordSys.json`);
  return await res.json();
}

export async function getBasemapProjWkt(url) {
  const res = await fetch(`${url}/prjCoordSys.wkt`);
  const file = await res.blob();
  return await file.text();
}

export async function getBasemapInfo(url) {
  const res = await fetch(`${url}.json`);
  return await res.json();
}
