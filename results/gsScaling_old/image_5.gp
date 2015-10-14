set terminal postscript eps enhanced color size 5.0,3.5
set output ".\\image_5.eps"
set size 1.0
set size ratio 0.5
set offsets 0.0 , 0.0, 0.0, 0.0 
set title "RGR / Cup / Loss / 5-anonymity"
set xlabel "Factor: Generalization / Suppression"
set ylabel "Runtime"
set key top left
set yrange[0.0:]
set grid
set xtics rotate by 0
set xtic scale 0
set style fill solid border -1
plot '.\\image_5.dat' using 1:2 with lines linecolor rgb "#1D4599" title col,\
     '' using 1:3 with lines linecolor rgb "#11AD34" title col,\
     '' using 1:4 with lines linecolor rgb "#E62B17" title col,\
     '' using 1:5 with lines linecolor rgb "#E69F17" title col