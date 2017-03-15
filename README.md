# storeboxapi
This program will use the unofficial REST API to get your receipt data from Storebox.com

Simply make your changes in the configurations file `src/main/resources/application.conf`, or create another file: `src/main/resources/local.application.conf` (latter is .gitignored), and run with `sbt run`.
Your data will be written to `output.tsv` in project root. First line of file is the data headers.

Ripe for analysis with most tools that will accept tsv files.

Enjoy!
