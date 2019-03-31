/* SQL-script to sort the species list of the TourCount Basic DB by code */

ALTER TABLE counts RENAME TO counts_old;

CREATE TABLE counts
( _id INTEGER,
  count_f1i int DEFAULT 0,
  count_f2i int DEFAULT 0,
  count_f3i int DEFAULT 0,
  count_pi int DEFAULT 0,
  count_li int DEFAULT 0,
  count_ei int DEFAULT 0,
  name text,
  code text,
  notes text DEFAULT "",
  name_g text DEFAULT "",
  PRIMARY KEY(_id)
);

INSERT INTO counts (name, code, name_g)
  SELECT name, code, name_g
  FROM counts_old
  order by code;

DROP TABLE counts_old;  

  