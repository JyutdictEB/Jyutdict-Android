# Jyutdict-Android  
## 泛粵大典   
An android version of [Jyutdict](https://www.jyutdict.org)  
  
Using APIs provided by [API List](https://www.jyutdict.org/api/)  
  
可檢索：通語字+音和泛粵字表中的錔字+綜合音+各地音（若有）+釋義，詳見 [關於泛粵大典](https://www.jyutdict.org/about)。  
各資料來源見應用內的說明頁，或"./app/src/main/assets/info/info.html"  
  
### 已知較明顯的bugs:  
在某些安卓4.4中從服務器接收到的json string是錯的；  
在主頁面按下返回后再開啟，應用會閃退：這是因爲避免新建出了兩個fragment而不得已臨時用了點trick帶來的副作用。  
  
## 版本歷史 ##  
### v0.2.6/200721  
首個公開的版本  