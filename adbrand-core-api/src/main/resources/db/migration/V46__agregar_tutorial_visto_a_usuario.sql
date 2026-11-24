-- Tutorial guiado de la web: si la empresa ya lo vio (o lo saltó), no se vuelve a mostrar solo,
-- aunque entre desde otra computadora. En "Mi cuenta" se puede volver a ver cuando quiera.
ALTER TABLE usuario
    ADD COLUMN tutorial_visto BOOLEAN NOT NULL DEFAULT FALSE;
