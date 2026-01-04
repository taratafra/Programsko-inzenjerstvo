# Pomna-sedmica
Projektni repozitorij za tim Pomna-sedmica za izradu aplikacije Mindfulness u kontekstu predmeta Programsko inžinjerstvo

# Članovi razvojnog time
* Tara Tafra, tara.tafra@fer.hr
* Ema Hofer, ema.hofer@fer.hr
* Martin Ante Rogošić, martin-ante.rogosic@fer.hr
* Zvonimir Sučić, zvonimir.sucic2@fer.hr
* Lovro Spajić, lovro.spajic@fer.hr
* Ivan Jakovljević, ivan.jakovljevic@fer.hr
* Ivan Lučić, ivan.lucic@fer.hr

# Linkovi
https://pomna-sedmica.onrender.com/login

# Lokalni deploy
NAPOMENA: Ne preporučamo lokalni deploy, no u slučaju da je neophodno morate se javiti Tara Tafra (tara.tafra@fer.hr) kako biste dobili api ključeve za potrebne vanjske servise

1. Korak iz kloniranog repozitorija prebacite se na granu prijaviSe
2. Korak poslane .env datoteke stavite u frontend/ i backend/ direktorije repozitorija
3. U backend direktoriju pokrenite sljedeće komande
   > cp .env src/main/resources/.env
   
   > mvn clean package
   > java -jar target/*.jar
4. U drugom terminalu pokrenite sljedeće komande
   > npm ci
   
   > npm start
5. Aplikacija je sada pokrenuta na http://localhost:3000

# Projektni zadatak
Cilj projekta je implementacija platforme minfulness za samoopomoć s mentalin zdravljem, raspoloženjem i meditaciju
