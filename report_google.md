# DAECC / Homework 01 / Team 3 / Solutions

These are the results for Google.

## What about IBM?

We tried out IBM and got both required functions up and running. The same day however, before we were able to measure anything 
our account was deactivated without further explanation. We've written an email, and are waiting for response. 

Until to that point working with IBM was either straight-forward, because the UI was super-intuitive, or a massive pain
as the documentation was very poor. 

## Task 2

The given code of [nqueens](https://github.com/sashkoristov/DAppMaster-2020W/tree/master/H02) was just taken as-is into Google Cloud Function. 

Notably to return a value in `javascript` one has to call ```res.send(object)``` with the return value, to return this value in the body to the callee.
Parameters to the function can be accessed with `req.query` for query parameters and `req.body` for a json given in a body. 

index.js: 
```javascript
const functions = require('@google-cloud/functions-framework');
const external = require('./external');

exports.fraction = async function (ev, res) {
    var from = parseInt(ev.params.from);
    var to = parseInt(ev.params.to);
    var num_queens = parseInt(ev.params.num_queens);
    var solutions = 0;
    for(var iter = from; iter < to; iter++){
        var code = iter;
        var queen_rows = [];
        for(var i = 0; i < num_queens; i++){
            queen_rows[i] = code % num_queens;
            code = Math.floor(code/num_queens);
        }
        if(external.acceptable(num_queens, queen_rows)){
            solutions += 1;
            console.log("Found valid placement: ", queen_rows);
        }
    }
    var result = { "solutions": solutions };
    return result;
}

functions.http('nqueens', (req, res) => {
    var num_queens = parseInt(req.query.num_queens);
    var from = parseInt(req.query.from);
    var to = parseInt(req.query.to);
    console.log("Running for placement range ", from, " to ", to);
    this.fraction({ params: { from: from, to: to, num_queens: num_queens } }).then(result => res.send(result));
});
```

external.js
```javascript
'use strict';

const _ = require('lodash')

exports.acceptable = function(num_queens, queen_rows){
    for(var i of _.range(0, num_queens)){
        for(var j of _.range(i + 1, num_queens)){
            if(queen_rows[i] == queen_rows[j]){
                return false;
            }
            if(queen_rows[i] - queen_rows[j] == i - j || queen_rows[i] - queen_rows[j] == j - i){
                return false;
            }
        }
    }
    return true;
}
```

## Task 3

```python
from google.cloud import storage
import time

def down_up(request):
    # args are query parameters
    source_bucket_name = request.args.get('source_bucket')
    target_bucket_name = request.args.get('target_bucket')
    filename = request.args.get('filename')
    storage_client = storage.Client()
    
    source_bucket = storage_client.bucket(source_bucket_name)

    start = time.time()
    blob = source_bucket.blob(filename)
    # use /tmp to store files
    blob.download_to_filename('/tmp/temp_file')

    inter = time.time()

    target_bucket = storage_client.bucket(target_bucket_name)
    upload_blob = target_bucket.blob(filename)
    upload_blob.upload_from_filename('/tmp/temp_file')

    end = time.time()

    # in python the function can just return body with a simple return
    return { 
        "upload_time": end - inter, 
        "download_time": inter - start
    }
```

## Task 6: NQueens with 8 Queens and 10⁸ Iterations

| Iteration | [RTT] = s      |
|-----------|----------------|
| 1         | 384.721        |
| 2         | 380.361        |
| 3         | <b>397.256</b> |
| 4         | 395.361        |
| 5         | 375.862        |
| 6         | 377.526        |
| 7         | 349.200        |
| 8         | 382.968        |
| 9         | 380.797        |
| 10        | 383.033        |

Although cold start can be seen for the average case, there are 2 executions which even took longer than cold start.
There probably is a reason for that, maybe some internal job scheduling, which unprioritizes long running jobs in favor of 
fast jobs with high memory capacaties. 

## Task 7: Download 10MB file from us-east (NV), upload to us-west (Oregon)

| Iteration | [Download Time]̣ = s | [Upload Time] = s | [RTT] = s  |
|-----------|----------------------|-------------------|------------|
| 1         | 4.4                  | 3.3               | 8.6        |
| 2         | 3.9                  | 2.9               | 6.9        |
| 3         | 3.8                  | 2.5               | 6.3        |
| 4         | 4                    | 2.9               | 6.9        |
| 5         | 4.8                  | 2.8               | 7.3        |
| 6         | 4.1                  | 2.9               | 7.1        |
| 7         | 4.3                  | 2.7               | 7          |
| 8         | 3.8                  | 2.5               | 6.3        |
| 9         | 4.1                  | 5.1               | <b>9.2</b> |
| 10        | 3.9                  | 2.9               | 6.9        |

Although cold vs. warm start is observable in the average case the maximum value occured in the ninth iteration. 
As this are I/O intensive workloads, one could guess that this might occur due to I/O or network congestions, packet loss, or some
other issue, during communication.

## Task 8

### Nqueens 8 GB - 8 Queens 10⁸ Iterations

| Iteration | [RTT] = s  |
|-----------|------------|
| 1         | 24.6       |
| 2         | 23.4       |
| 3         | 22.0       |
| 4         | 23.9       |
| 5         | 21.9       |
| 6         | 23.2       |
| 7         | 23.6       |
| 8         | 24.8       |
| 9         | 23.9       |
| 10        | 23.4       |



### Download 10MB file from us-east (NV), upload to us-west (Oregon) (8 GB memory)

| Iteration | [Download Time]̣ = s | [Upload Time] = s | [RTT] = s |
|-----------|----------------------|-------------------|----------|
| 1         | 0.46                 | 2.41              | 4.3      |
| 2         | 0.6                  | 1.44              | 2.1      |
| 3         | 0.39                 | 1.48              | 1.9      |
| 4         | 0.44                 | 1.9               | 2.4      |
| 5         | 0.46                 | 1.57              | 2.1      |
| 6         | 0.56                 | 2.38              | 3        |
| 7         | 0.52                 | 1.48              | 2        |
| 8         | 0.41                 | 1.37              | 1.9      |
| 9         | 0.49                 | 1.45              | 2        |
| 10        | 0.46                 | 1.43              | 1.9      |

In both cases the functions showed substantial speedup (nearly 10x) for Nqueens.

Download and upload of course depends on the physical connection across the ocean. The 2x speedup is however still considerable
and one might need to investigate further why that is. One guess might be, that the function encodes the content before it gets sent, 
therefore showing speedup for larger memory. 

As also hinted before, we could also imagine, that there are some internal scheduling policies, rewarding more expensive functions.

Cold vs. Warm start is still observable (about 2 second differences), even better than for the restricted memory functions.