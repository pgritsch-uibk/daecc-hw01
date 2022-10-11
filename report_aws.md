# Task 6

## RTT measurements (output)

    Iteration 1: 28557ms
    Iteration 2: 22984ms
    Iteration 3: 22421ms
    Iteration 4: 22396ms
    Iteration 5: 22382ms
    Iteration 6: 22379ms
    Iteration 7: 22398ms
    Iteration 8: 22346ms
    Iteration 9: 22414ms
    Iteration 10: 22278ms
    Completed! Total elapsed time: 228555ms
    
    Iteration 1: 24526ms
    Iteration 2: 22440ms
    Iteration 3: 22315ms
    Iteration 4: 22323ms
    Iteration 5: 22340ms
    Iteration 6: 22339ms
    Iteration 7: 22359ms
    Iteration 8: 22400ms
    Iteration 9: 22414ms
    Iteration 10: 22459ms
    Completed! Total elapsed time: 225915ms

## Discussion

- Iteration 1: longer than the other iterations (+6s)
- This is probably due to a cold start
- Iteration 1 of the second run seems to be affected too (+2s)

# Task 7

## RTT measurements (output)

    Iteration 1: 23907ms
    Iteration 2: 4744ms
    Iteration 3: 3626ms
    Iteration 4: 3945ms
    Iteration 5: 3485ms
    Iteration 6: 3475ms
    Iteration 7: 3497ms
    Iteration 8: 3219ms
    Iteration 9: 3270ms
    Iteration 10: 3023ms
    Completed! Total elapsed time: 56191ms
    
    Iteration 1: 7900ms
    Iteration 2: 3082ms
    Iteration 3: 3568ms
    Iteration 4: 3214ms
    Iteration 5: 3061ms
    Iteration 6: 3171ms
    Iteration 7: 3191ms
    Iteration 8: 3175ms
    Iteration 9: 3641ms
    Iteration 10: 3072ms
    Completed! Total elapsed time: 37075ms

## Discussion

- Again iteration 1: by far the longest (+20s)
- This time the difference is even bigger
    - Maybe because of the usage of S3 in the lambda function which may have a longer initialization time (due to e.g. partitioning) when accessed the first time (?)

# Task 8

Both lambda functions were set to 3008mb ram (seems to be the maximum for our aws accounts)

## RTT measurements (nqueens)

    Iteration 1: 4007ms
    Iteration 2: 1705ms
    Iteration 3: 1649ms
    Iteration 4: 1701ms
    Iteration 5: 1661ms
    Iteration 6: 1654ms
    Iteration 7: 1650ms
    Iteration 8: 1650ms
    Iteration 9: 1652ms
    Iteration 10: 1650ms
    Completed! Total elapsed time: 18979ms
    
    Iteration 1: 3668ms
    Iteration 2: 1661ms
    Iteration 3: 1655ms
    Iteration 4: 1650ms
    Iteration 5: 1647ms
    Iteration 6: 1646ms
    Iteration 7: 1645ms
    Iteration 8: 1651ms
    Iteration 9: 1658ms
    Iteration 10: 1658ms
    Completed! Total elapsed time: 18539ms

## RTT measurements (downUp)

    Iteration 1: 4209ms
    Iteration 2: 2081ms
    Iteration 3: 2015ms
    Iteration 4: 2018ms
    Iteration 5: 1882ms
    Iteration 6: 1997ms
    Iteration 7: 1873ms
    Iteration 8: 2052ms
    Iteration 9: 1943ms
    Iteration 10: 2024ms
    
    Completed! Total elapsed time: 22094ms
    Iteration 1: 3426ms
    Iteration 2: 2130ms
    Iteration 3: 2142ms
    Iteration 4: 2034ms
    Iteration 5: 1850ms
    Iteration 6: 1896ms
    Iteration 7: 2066ms
    Iteration 8: 2003ms
    Iteration 9: 1916ms
    Iteration 10: 1914ms
    Completed! Total elapsed time: 21377ms

## Discussion

- RTT of nqueens is way shorter
- Speedup:
    - First iteration: 7.13
    - Other iterations: ~13.5
- This may be due to the intensive problem
- RTT of downUp is shorter but the speedup is not as high as for nqueens
- Speedup: 
    - First iteration: 5.67
    - Other iterations: ~1.75