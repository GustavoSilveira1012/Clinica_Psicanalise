const configuredClinicalOnly = import.meta.env.VITE_CLINICAL_ONLY_PILOT;

export const clinicalOnlyPilot = import.meta.env.PROD
  ? configuredClinicalOnly !== "false"
  : configuredClinicalOnly === "true";

const configuredClinicalData = import.meta.env.VITE_CLINICAL_DATA_ENABLED;

export const clinicalDataEnabled = import.meta.env.PROD
  ? configuredClinicalData === "true"
  : configuredClinicalData !== "false";

export const clinicalDataRoutes = new Set([
  "/dashboard",
  "/agenda",
  "/patients",
  "/clinical-records",
  "/finance",
  "/compliance",
]);

export const pilotUnavailableRoutes = new Set([
  "/packages",
  "/subscriptions",
  "/fiscal",
  "/notifications",
]);
