# PeriodicalRandomSortField

* Provides RandomSortField periodically updated.
* Unlike RandomSortField, this is does not depend on docBase and index version.
* Using queryResultCache, sort update time is delay. In that case, added FilterQuery that is alwayes true and include unixtime(Ex: `fq=hoge_i:[* TO {UNIXTIME}]`).

定期的に更新されるランダムソートフィールドが作れます。

標準のRandomSortFiledはdocBaseかインデックスのバージョンが変わるとソート順がリセットされますが、このソートフィールドはリセットされません。リセットされるのはシード値を変えた場合と指定した時刻が過ぎたときです。

なおqueryResultCacheを使っていると指定した時刻ちょうどにソート順がリセットされない場合があります。その場合はFilterQueryで常に真かつunixtimeを含む条件（`fq=hoge_i:[* TO {UNIXTIME}]`）つけるなどすると回避できます（バッドノウハウですが）。

## INSTALL

load jar file (solrconfig)

```
wget periodical-random-sort-field-XXX.jar
cp periodical-random-sort-field-XXX.jar {SOLR_DIR}/dist


cd {SOLR_CORE_CONF}
vi solrconfig.xml

...
  <lib dir="{SOLR_DIR}/dist/" regex="periodical-random-sort-field-.*\.jar" />
...
```

create sort field (schema.xml)

```
vi schema.xml

...
    <fieldType name="prandom" class="com.bengo4.solr.schema.PeriodicalRandomSortField" indexed="true" />

    <dynamicField name="prandom_*" type="prandom" />
...
```


# Usage

Usage: `prandom_{SEED}[_{PERIODS}[_{EPOC_TIME}]] desc|asc`

* {SEED} : Random seed
* {PERIODS} : Optional. update timing. minuts or hours. (ex 5m,4h)
* {EPOC_TIME} : Optional. period epoc unixtime (Defaults today 00:00:00)



# Example
`http://localhost:8983/solr/{YOUR_CORE}/select?q=*:*&prandom_{SEED}_{PERIODS}_{EPOC_TIME}+desc`


* No EPOC_TIME, No periods.
   
      ...&prandom_SEED+desc


* EPOC_TIME is todays 00:00:00, updating 12 o'clock

      ...&prandom_SEED_12h+desc


* EPOC_TIME is 2015-02-21 4:00:00, updating after 4 hour and 8 hour, 12 hour.

      ...&prandom_SEED_4h,8h,12h_1424458800+desc



# License

Copyright (c) 2015 MISUMI Masaru (deltam@gmail.com).

Licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php)



