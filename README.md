# Multi omics data integration tool
IntOmics is a tool for integrating proteomics and transcriptomics human data in order to uncover cell crosstalk mechanisms.

---------------

### Usage
`java -jar intOmics.jar -p <file> -g <file> -a <file> [options]*`

### Parameters
| Argument          | Description                                           |
|:-----------------:|:-----------------------------------------------------:|
| -p \<file>        | Text file containing secreted proteins                |
| -g \<file>        | Text file containing differentially expressed genes   |
| -a \<file>        | Text file containing all expressed genes              |
| -f \<string>      | Output format: TSV or FWF                             |
| -db \<string>     | Pathway database: [KEGG, WikiPathways, Reactome]      |
| -s \<int>         | Minimum score for PPI (range from 0 to 999)           |
| -fc \<double>     | Fold change cutoff                                    |
| -pv \<double>     | P-value cutoff                                        |
| -t \<int>         | Number of threads to use                              |
| -d \<char>        | Custom separator for input files                      |
| -i                | Pull an up to date list of pathways                   |
| -h                | Print the help screen                                 |

--------------------

### Input files
##### Secreted proteins


##### Differentially expressed genes






