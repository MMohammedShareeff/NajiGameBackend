UPDATE scenario_bank SET text = replace(text, 'الجيلي', 'الهلام') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'روبوت لطيف', 'إنسان آلي لطيف') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'روبوت عملاق وسحلية', 'إنسان آلي عملاق وسحلية') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'ملك روبوت', 'ملك آلي') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'الروبوتات تسيطر على المدينة، والمكان الوحيد بلا روبوتات هو', 'البشر الآليون يسيطرون على المدينة، والمكان الوحيد الذي لا يوجد فيه بشر آليون هو') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'الفرسان والروبوتات والديناصورات', 'الفرسان والبشر الآليون والديناصورات') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'ترامبولين عملاق', 'نطاطة عملاقة') WHERE lang = 'ar';
UPDATE scenario_bank SET text = replace(text, 'لعبة فيديو', 'لعبة إلكترونية') WHERE lang = 'ar';
UPDATE scenario_bank SET theme = 'فكاهة' WHERE lang = 'ar' AND theme = 'كوميديا';
