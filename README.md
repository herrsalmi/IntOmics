# Multi omics data integration tool
IntOmics is a tool for integrating proteomics and transcriptomics human data in order to uncover cell crosstalk mechanisms.

---------------

### Usage
`java -jar intOmics.jar -p <file> -a <file> [options]*`

### Parameters
| Argument          | Description                                           |
|:-----------------:|:-----------------------------------------------------:|
| -p \<file>        | Text file containing secreted proteins                |
| -g \<file>        | Text file containing DE testing for all genes         |
| -f \<string>      | Output format: TSV or FWF                             |
| -db \<string>     | Pathway database: [KEGG, WikiPathways, Reactome]      |
| -s \<int>         | Minimum score for PPI (range from 0 to 999)           |
| -fc \<double>     | Fold change cutoff                                    |
| -pv \<double>     | P-value cutoff                                        |
| -t \<int>         | Number of threads to use                              |
| -d \<string>      | Custom separator for input files                      |
| -i                | Pull an up to date list of pathways                   |
| -h                | Print the help screen                                 |

--------------------

### Input files
Input files should be in CSV format and can have a header line starting with `#`. The default column separator is `;`, but a different one can be specified using the argument `-d`.
##### Secreted proteins
Text file containing protein names or corresponding Entrez gene id, each on in a separate line.

##### Differential expression testing results
Text file in CSV format with three columns: `gene name`, `p value` and `fold change`. 







