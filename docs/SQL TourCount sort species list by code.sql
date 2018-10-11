/* SQL-script to sort the TourCount species list by code */

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
  PRIMARY KEY(_id)
);

INSERT INTO counts (count_f1i, count_f2i, count_f3i, count_pi, count_li, count_ei, name, code, notes)
  SELECT count_f1i, count_f2i, count_f3i, count_pi, count_li, count_ei, name, code, notes
  FROM counts_old
  order by code;

DROP TABLE counts_old;  
