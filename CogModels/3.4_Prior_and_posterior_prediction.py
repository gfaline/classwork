import arviz as az
import pymc3 as pm
import matplotlib.pyplot as plt
import numpy as np
from scipy import stats

## We're going to ignore some warnings that pymc3 can give
import warnings
warnings.simplefilter(action="ignore", category=FutureWarning)

## 3.4

## In this example, we will sample from the
## prior predictive and posterior predictive
## distributions.

## These the the predictions about data before and
## after we update our parameter beliefs, respectively

# Data
## This is another binomial example
## with 15 trials and 1 success
n_successes = 1
n_trials = 15

with pm.Model() as model:
    ## We will start with an uninformative Beta(1,1) prior
    ## which gives equal probability to all success chances
    ## between 0 and 1
    success_rate = pm.Beta("success_rate", alpha=1, beta=1)

    # The process is binary (Bernoulli). As discussed,
    # a Bernoulli process that happens several times
    # is the Binomial process.
    # In this case, we had `n_trials` with `n_successes`.
    # Our prior for the success rate comes from `success_rate` above
    outcome = pm.Binomial("outcome", n=n_trials, p=success_rate,
                          observed=n_successes)

    ## We sample the prior predictive
    ## This is how we would expect our empirical data to look
    ## Based on our prior belief
    prior_predictions = pm.sample_prior_predictive()

    ## Next, we update the model based on our data:
    trace = pm.sample()

    ## Then, we can sample the posterior predictive
    ## Which is what we would predict empirical data to
    ## look like based on our updated belief
    ## Notice that the trace is a parameter of this function.
    posterior_predictions = pm.sample_posterior_predictive(trace)

## Plot the posterior predictions
## This is the simulated successes in 15 trials with
## prior beliefs
az.plot_dist(prior_predictions["outcome"], show=True)

## Plot the posterior predictions
## This is the simulated successes in 15 trials with
## updated posterior beleifs
az.plot_dist(posterior_predictions["outcome"], show=True)

## Below is the code for reproducing the charts in Lee & Wagenmakers
## Which has a graph on top for prior and posterior `success_rate` belief
## and a histogram on bottom for prior and posterior predictive `outcome`

prior_outcome = prior_predictions["outcome"]
predictive_success_rate = trace["success_rate"]

## plt.subplots returns two items, but we don't need the first
fig, axes = plt.subplots(2, 1, figsize=(6, 5))

## First, add the posterior `predictive_success_rate` in red
az.plot_kde(predictive_success_rate, ax=axes[0],
            plot_kwargs={"color": "r"}, label="Posterior")
x = np.linspace(0, 1, 100)
# Then the prior in blue
axes[0].plot(x, stats.beta.pdf(x, 1, 1), color="b", label="Prior")
axes[0].set_xlabel("Rate")
axes[0].set_ylabel("Density")
axes[0].legend()

predictive_outcome = posterior_predictions["outcome"]
## axes[1] is the second chart
## Again, the  posterior is first, in red
axes[1].hist(
    predictive_outcome,
    density=1,
    bins=len(np.unique(predictive_outcome)),
    alpha=0.3,
    color="r",
    label="Posterior",
)
## and the prior is in blue
axes[1].hist(prior_outcome, density=1, bins=n_trials + 1, alpha=0.3, color="b", label="Prior")
axes[1].set_xlabel("Success Count")
axes[1].set_ylabel("Mass")
axes[1].legend()

plt.tight_layout();
plt.show()
