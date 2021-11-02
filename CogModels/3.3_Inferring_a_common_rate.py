import arviz as az
import pymc3 as pm

## We're going to ignore some warnings that pymc3 can give
import warnings
warnings.simplefilter(action="ignore", category=FutureWarning)

## 3.3

## We now have one process, which we've monitored twice for output
## Each iteration has its own number of trials and number of successes
## but the underlying process was the same.

# Data
## First, we did 10 trials and got 5 successes.
n_first_trials = 10
n_first_successes = 5
## Then we did 9 trials and got 7 successes
n_second_trials = 9
n_second_successes = 7

n_successes = [n_first_successes, n_second_successes]
n_trials = [n_first_trials, n_second_trials]

with pm.Model() as model:
    # Our prior is the uninformative Beta(1,1),
    # which gives equal probability to all numbers
    # between 0 and 1
    success_rate = pm.Beta("success_rate", alpha=1, beta=1)
    
    # The process is binary (Bernoulli). As discussed,
    # a Bernoulli process that happens several times
    # is the Binomial process.
    # We have compiled both trials into `n_trials` and both
    # success counts into `n_successes`
    outcome = pm.Binomial("outcome", n=n_trials, p=success_rate,
                          observed=n_successes)
    
    # Finally, we perform the Bayesian inference step
    # The output of sampling includes the entire "trace" of the sample,
    # which has plenty of information for trouble-shooting as well as our
    # posterior prediction
    trace = pm.sample()

## Plot our success_rate posterior distribution
az.plot_dist(trace["success_rate"], show=True)

## Finally, print details like mean, standard deviation
## and confidence intervals to the terminal
print(az.summary(trace, var_names=["success_rate"], round_to=3))
